package client.wlx

import client.MwBot
import client.slick.Slick
import client.wlx.dto.{Contest, SpecialNomination}
import client.wlx.query.{ImageQuery, ImageQueryApi, MonumentQuery}

class Statistics {

  import scala.concurrent.ExecutionContext.Implicits.global

  val slick = new Slick()

  val wlmContest = Contest.WLMUkraine(2014, "09-15", "10-15")
  val previousContests = (2012 to 2013).map(year => Contest.WLMUkraine(year, "09-01", "09-30"))

  def init(): Unit = {

    val monumentQuery = MonumentQuery.create(wlmContest)

    val allMonuments = monumentQuery.byMonumentTemplate(wlmContest.listTemplate)
    val monumentDb = new MonumentDB(wlmContest, allMonuments)

    //    saveMonuments(monumentDb)

    val imageQueryDb = ImageQuery.create(db = true)
    val imageQueryApi = ImageQuery.create(db = false)

    for (imageDb <- ImageDB.create(wlmContest, imageQueryApi, monumentDb)) {

      authorsStat(monumentDb, imageDb)
      specialNominations(wlmContest, imageDb, monumentQuery)

      val total = new ImageQueryApi().imagesWithTemplateAsync(wlmContest.fileTemplate, wlmContest)
      for (totalImages <- total) {

        val totalImageDb = new ImageDB(wlmContest, totalImages, monumentDb)

        regionalStat(wlmContest, monumentDb, imageQueryDb, imageDb, totalImageDb)
        fillLists(monumentDb, totalImageDb)
      }
    }

  }

  def authorsStat(monumentDb: MonumentDB, imageDb: ImageDB) {
    val output = new Output()
    val text = output.authorsMonuments(imageDb)
    MwBot.get(MwBot.commons).page("Commons:Wiki Loves Monuments 2014 in Ukraine/Number of objects pictured by uploader").edit(text, "updating")
  }

  def regionalStat(wlmContest: Contest, monumentDb: MonumentDB,
                   imageQueryDb: ImageQuery,
                   currentYear: ImageDB,
                   totalImageDb: ImageDB) {

    val dbsByYear = previousContests.map(contest => ImageDB.create(contest, imageQueryDb, monumentDb))

    dbsByYear.head.map {
      firstYear =>
        dbsByYear.last.map {
          lastYear =>

            val imageDbs = Seq(firstYear, lastYear, currentYear)
            //      initImages()
            //      for (imageDb <- imageDbs) {
            //        saveImages(imageDb)
            //      }

            val output = new Output()

            val idsStat = output.monumentsPictured(imageDbs, totalImageDb, monumentDb)
            println(idsStat)

            val authorStat = output.authorsContributed(imageDbs, totalImageDb, monumentDb)
            println(authorStat)

            val toc = "__TOC__"
            val category = "\n[[Category:Wiki Loves Monuments 2014 in Ukraine]]"
            val regionalStat = toc + idsStat + authorStat + category

            //      val bot = MwBot.get(MwBot.commons)
            //      bot.await(bot.page("Commons:Wiki Loves Monuments 2014 in Ukraine/Regional statistics").edit(regionalStat, "update statistics"))

            MwBot.get(MwBot.commons).page("Commons:Wiki Loves Monuments 2014 in Ukraine/Regional statistics").edit(regionalStat, "updating")

            val monumentQuery = MonumentQuery.create(wlmContest)

            specialNominations(previousContests.find(_.year == 2012).get, firstYear, monumentQuery)
            specialNominations(previousContests.find(_.year == 2013).get, lastYear, monumentQuery)

        }
    }
  }

  def fillLists(monumentDb: MonumentDB, imageDb: ImageDB): Unit = {
    new ListFiller().fillLists(monumentDb, imageDb)
  }

  def saveMonuments(monumentDb: MonumentDB) {
    import scala.slick.driver.H2Driver.simple._

    slick.db.withSession { implicit session =>
      slick.monuments.ddl.drop
      slick.monuments.ddl.create

      slick.monuments ++= monumentDb.allMonuments
    }
  }

  def initImages() {

    slick.db.withSession { implicit session =>
      //      slick.images.ddl.drop
      //      slick.images.ddl.create
    }
  }

  def saveImages(imageDb: ImageDB) {
    import scala.slick.driver.H2Driver.simple._

    slick.db.withSession { implicit session =>
      slick.images ++= imageDb.images
    }
  }

  def specialNominations(contest: Contest, imageDb: ImageDB, monumentQuery: MonumentQuery) {
    val monumentsMap = SpecialNomination.nominations.map { nomination =>
      val monuments = monumentQuery.byPage(nomination.pages.head, nomination.listTemplate)
      (nomination, monuments)
    }.toMap

    val allMonuments = monumentsMap.values.flatMap(identity)

      val imageDbs: Map[SpecialNomination, ImageDB] = SpecialNomination.nominations.map { nomination =>
        (nomination, imageDb.subSet(monumentsMap(nomination)))
      }.toMap

      val output = new Output()
      val stat = output.specialNomination(contest, imageDbs)

      MwBot.get(MwBot.commons).page(s"Commons:Wiki Loves Monuments ${contest.year} in Ukraine/Special nominations statistics").edit(stat, "updating")
  }


}

object Statistics {
  def main(args: Array[String]) {
    new Statistics().init()
  }
}
