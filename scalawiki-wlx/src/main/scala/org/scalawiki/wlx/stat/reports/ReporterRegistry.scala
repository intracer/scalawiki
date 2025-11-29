package org.scalawiki.wlx.stat.reports

import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.stat.rating.Rater
import org.scalawiki.wlx.stat.{ContestStat, StatConfig, Stats}
import org.scalawiki.wlx.{ImageDB, ImageFiller, MonumentDB}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ReporterRegistry(stat: ContestStat, cfg: StatConfig)(implicit
    ec: ExecutionContext
) {

  import org.scalawiki.wlx.stat.reports.{ReporterRegistry => RR}

  private val contest = stat.contest
  private val monumentDb = stat.monumentDb
  private val currentYearImageDb = stat.currentYearImageDb
  private val totalImageDb = stat.totalImageDb
  private val commons = MwBot.fromHost(MwBot.commons)

  def monumentDbStat: Option[String] = stat.monumentDb.map(RR.monumentDbStat)

  def when(cond: Boolean)(f: => Future[_]): Future[Any] = if (cond) f else Future.successful()

  //  def authorsMonuments: String =
  //    RR.authorsMonuments(stat.currentYearImageDb.get)

  def authorsImages: String =
    RR.authorsImages(currentYearImageDb, monumentDb)

  def authorsContributed: String =
    RR.authorsContributed(stat.dbsByYear, totalImageDb, monumentDb)

  def specialNominations(): String = RR.specialNominations(stat)

  def mostPopularMonuments: String = new MostPopularMonuments(stat).asText

  def monumentsPictured: String =
    new MonumentsPicturedByRegion(stat, gallery = true).asText

  def withArticles: Option[String] = RR.withArticles(monumentDb)

  /** Outputs current year reports.
    */
  def currentYear(): Future[Unit] = {
    val imageDb = currentYearImageDb

    for {
      _ <- new RecentlyTaken(stat).updateWiki(commons)
      _ <- when(cfg.specialNominations) {
        new SpecialNominations(stat, imageDb).statistics()
      }
      _ <- when(cfg.lowRes) {
        Output.lessThan2MpGallery(contest, imageDb)
      }
      mDb <- monumentDb
        .map(Future.successful)
        .getOrElse(Future.failed(new IllegalStateException("Monument DB is required")))
      _ <- when(cfg.wrongIds) {
        Output.wrongIds(imageDb, mDb)
      }

      _ <- when(cfg.missingIds) {
        Output.missingIds(imageDb, mDb)
      }

      _ <- when(cfg.multipleIds) {
        Output.multipleIds(imageDb, mDb)
      }

      _ <- when(cfg.fillLists && cfg.years.size == 1) {
        ImageFiller.fillLists(mDb, imageDb)
      }

      _ <- when(cfg.missingGallery) {
        Output.missingGallery(mDb)
      }

      _ <- when(cfg.placeDetection) {
        Output.unknownPlaces(mDb, imageDb)
        Output.unknownPlaces(mDb)
      }

      _ <- when(cfg.mostPopularMonuments) {
        new MostPopularMonuments(stat).updateWiki(
          MwBot.fromHost(MwBot.commons)
        )
      }
    } yield ()
  }

  def allYears(): Future[Unit] = {
    val imageDb = totalImageDb

    for {
      _ <- when(cfg.fillLists) {
        ImageFiller.fillLists(monumentDb.get, imageDb)
      }

      _ <- when(cfg.regionalStat) {
        Output.regionalStat(stat)
      }

      _ <- when(cfg.newMonuments) {
        Output.newMonuments(stat)
      }

      _ <- when(cfg.authorsStat) {
        new AuthorsStat().authorsStat(stat, commons, cfg.gallery)
      }
      _ <- when(cfg.rateInputDistribution) {
        Future {
          Rater.create(stat)
        }
      }

      _ <- when(cfg.regionalGallery) {
        Output.byRegion(monumentDb.get, imageDb)
      }

      _ <- when(cfg.numberOfMonumentsByNumberOfPictures) {
        // new NumberOfMonumentsByNumberOfPictures(stat, imageDb).updateWiki(commons)
        val mDb = monumentDb.get
        Future { Gallery.gallery(imageDb, mDb) }
      }
    } yield ()
  }

  def output(): Future[Unit] = {
    for {
      _ <- currentYear()
      _ <- allYears()
    } yield ()
  }

}

class NumberOfMonumentsByNumberOfPictures(
    val stat: ContestStat,
    val imageDb: ImageDB
) extends Reporter {
  val picturesPerMonument =
    imageDb.images.flatMap(_.monumentIds).groupBy(identity).values.map(_.size)
  val numberOfMonumentsByNumberOfPictures = picturesPerMonument
    .groupBy(identity)
    .mapValues(_.size)
    .toSeq
    .sortBy { case (pictures, monuments) => -pictures }

  override val table =
    Table(
      Seq("pictures", "monuments"),
      numberOfMonumentsByNumberOfPictures.map { case (pictures, monuments) =>
        Seq(pictures.toString, monuments.toString)
      }
    )

  override def name: String = "Number Of monuments by number of pictures"
}

object ReporterRegistry {

  def monumentDbStat(db: MonumentDB): String =
    new MonumentDbStat().getStat(Seq(db))

  //  def authorsMonuments(imageDb: ImageDB, newObjectRating: Option[Int] = None): String =
  //    new AuthorMonuments(imageDb, newObjectRating).asText

  def authorsImages(imageDb: ImageDB, monumentDb: Option[MonumentDB]): String =
    new AuthorsStat().authorsImages(imageDb._byAuthor.grouped, monumentDb)

  def authorsContributed(
      imageDbs: Seq[ImageDB],
      totalImageDb: ImageDB,
      monumentDb: Option[MonumentDB]
  ): String = new AuthorsStat().authorsContributed(imageDbs, totalImageDb, monumentDb)

  def specialNominations(stat: ContestStat): String =
    new SpecialNominations(stat, stat.currentYearImageDb).specialNomination()

  def withArticles(monumentDb: Option[MonumentDB]): Option[String] =
    monumentDb.map(db => Stats.withArticles(db).asWiki("").asWiki)
}
