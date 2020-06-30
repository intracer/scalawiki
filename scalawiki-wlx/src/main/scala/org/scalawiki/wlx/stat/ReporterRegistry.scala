package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.wlx.{ImageDB, ImageFiller, MonumentDB}

import scala.concurrent.ExecutionContext

class ReporterRegistry(stat: ContestStat, cfg: StatConfig)(implicit ec: ExecutionContext) {
  import org.scalawiki.wlx.stat.{ReporterRegistry => RR}

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

  def specialNominations(): String = RR.specialNominations(currentYearImageDb.get)

  def mostPopularMonuments: String = new MostPopularMonuments(stat).asText

  def monumentsPictured: String = new MonumentsPicturedByRegion(stat).asText

  def withArticles: Option[String] = RR.withArticles(monumentDb)

  /**
    * Outputs current year reports.
    *
    */
  def currentYear() = {
    for (imageDb <- currentYearImageDb) {

      if (cfg.specialNominations) {
        new SpecialNominations(contest, imageDb).statistics()
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

        if (cfg.fillLists) {
          ImageFiller.fillLists(mDb, imageDb)
        }

        if (cfg.missingGallery) {
          Output.missingGallery(mDb)
        }

        if (cfg.placeDetection) {
          Output.unknownPlaces(mDb, imageDb)
          Output.unknownPlaces(mDb)
        }
      }
    }
  }

  def allYears() = {
    for (imageDb <- totalImageDb) {
      if (cfg.regionalStat) {
        Output.regionalStat(stat)
      }

      if (cfg.authorsStat) {
        new AuthorsStat().authorsStat(stat, commons, cfg.gallery)
      }

      if (cfg.regionalGallery) {
        Output.byRegion(monumentDb.get, imageDb)
      }
    }
  }

  def output() = {
    currentYear()
    allYears()
  }

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

  def specialNominations(imageDB: ImageDB): String =
    new SpecialNominations(imageDB.contest, imageDB).specialNomination()

  def withArticles(monumentDb: Option[MonumentDB]): Option[String] =
    monumentDb.map(db => Stats.withArticles(db).asWiki("").asWiki)
}



