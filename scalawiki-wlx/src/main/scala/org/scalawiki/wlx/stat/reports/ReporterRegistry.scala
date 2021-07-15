package org.scalawiki.wlx.stat.reports

import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.stat.{ContestStat, Rater, StatConfig, Stats}
import org.scalawiki.wlx.{ImageDB, ImageFiller, MonumentDB}

import scala.concurrent.ExecutionContext
import scala.util.Try

class ReporterRegistry(stat: ContestStat, cfg: StatConfig)(implicit ec: ExecutionContext) {

  import org.scalawiki.wlx.stat.reports.{ReporterRegistry => RR}

  val contest = stat.contest
  val monumentDb = stat.monumentDb
  val currentYearImageDb = stat.currentYearImageDb
  val totalImageDb = stat.totalImageDb
  val commons = MwBot.fromHost(MwBot.commons)

  def monumentDbStat: Option[String] = stat.monumentDb.map(RR.monumentDbStat)

  //  def authorsMonuments: String =
  //    RR.authorsMonuments(stat.currentYearImageDb.get)

  def authorsImages: String = RR.authorsImages(currentYearImageDb.get, monumentDb)

  def authorsContributed: String = RR.authorsContributed(stat.dbsByYear, totalImageDb, monumentDb)

  def specialNominations(): String = RR.specialNominations(stat)

  def mostPopularMonuments: String = new MostPopularMonuments(stat).asText

  def monumentsPictured: String = new MonumentsPicturedByRegion(stat).asText

  def withArticles: Option[String] = RR.withArticles(monumentDb)

  /**
   * Outputs current year reports.
   *
   */
  def currentYear() = {
    for (imageDb <- currentYearImageDb) {

      if (cfg.regionalGallery && stat.totalImageDb.isEmpty) {
        Output.byRegion(monumentDb.get, imageDb)
      }

      if (cfg.specialNominations) {
        new SpecialNominations(stat, imageDb).statistics()
      }

      if (cfg.lowRes) {
        Output.lessThan2MpGallery(contest, imageDb)
      }

      monumentDb.foreach { mDb =>
        if (cfg.wrongIds) {
          Output.wrongIds(imageDb, mDb)
        }

        if (cfg.missingIds) {
          Output.missingIds(imageDb, mDb)
        }

        if (cfg.multipleIds) {
          Output.multipleIds(imageDb, mDb)
        }

        //        if (cfg.fillLists) {
        //          ImageFiller.fillLists(mDb, imageDb)
        //        }

        if (cfg.missingGallery) {
          Output.missingGallery(mDb)
        }

        if (cfg.placeDetection) {
          Output.unknownPlaces(mDb, imageDb)
          Output.unknownPlaces(mDb)
        }

        if (cfg.mostPopularMonuments) {
          new MostPopularMonuments(stat).updateWiki(MwBot.fromHost(MwBot.commons))
        }
      }
    }
  }

  def allYears() = {
    for (imageDb <- totalImageDb) {
      if (cfg.fillLists) {
        ImageFiller.fillLists(monumentDb.get, new ImageDB(stat.contest, stat.oldImages, monumentDb))
      }

      if (cfg.regionalStat) {
        Output.regionalStat(stat)
      }

      if (cfg.newMonuments) {
        Output.newMonuments(stat)
      }

      if (cfg.authorsStat) {
        new AuthorsStat().authorsStat(stat, commons, cfg.gallery)
      } else if (cfg.rateInputDistribution) {
        Rater.create(stat)
      }

      if (cfg.regionalGallery) {
        Output.byRegion(monumentDb.get, imageDb)
      }

      if (cfg.numberOfMonumentsByNumberOfPictures) {
        // new NumberOfMonumentsByNumberOfPictures(stat, imageDb).updateWiki(commons)
        val mDb = monumentDb.get

        Gallery.gallery(imageDb, mDb)
      }
    }
  }

  def output(): Unit = {
    currentYear()
    allYears()
  }

}

class NumberOfMonumentsByNumberOfPictures(val stat: ContestStat, val imageDb: ImageDB) extends Reporter {
  val picturesPerMonument = imageDb.images.flatMap(_.monumentIds).groupBy(identity).values.map(_.size)
  val numberOfMonumentsByNumberOfPictures = picturesPerMonument.groupBy(identity).mapValues(_.size).toSeq
    .sortBy { case (pictures, monuments) => -pictures }

  override val table =
    Table(
      Seq("pictures", "monuments"),
      numberOfMonumentsByNumberOfPictures.map { case (pictures, monuments) => Seq(pictures.toString, monuments.toString) }
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

  def authorsContributed(imageDbs: Seq[ImageDB], totalImageDb: Option[ImageDB], monumentDb: Option[MonumentDB]): String =
    new AuthorsStat().authorsContributed(imageDbs, totalImageDb, monumentDb)

  def specialNominations(stat: ContestStat): String =
    new SpecialNominations(stat, stat.currentYearImageDb.get).specialNomination()

  def withArticles(monumentDb: Option[MonumentDB]): Option[String] =
    monumentDb.map(db => Stats.withArticles(db).asWiki("").asWiki)
}



