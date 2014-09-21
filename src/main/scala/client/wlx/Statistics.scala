package client.wlx

import client.slick.Slick
import client.wlx.dto.{Contest, SpecialNomination}
import client.wlx.query.{ImageQuery, MonumentQuery}

import scala.concurrent.Future

class Statistics {

  import scala.concurrent.ExecutionContext.Implicits.global

  val slick = new Slick()

  def init(): Unit = {
    val wlmContest = Contest.WLMUkraine(2014, "09-15", "10-15")
    val allContests = (2013 to 2013).map(year => Contest.WLMUkraine(year, "09-01", "09-30"))

    val monumentQuery = MonumentQuery.create(wlmContest)

    val allMonuments = monumentQuery.byMonumentTemplate(wlmContest.listTemplate)
    val monumentDb = new MonumentDB(wlmContest, allMonuments)

    saveMonuments(monumentDb)

   val imageQuery = ImageQuery.create()
   regionalStat(wlmContest, allContests, monumentDb, imageQuery)

    //    specialNominations(allContests.find(_.year == 2013).get, imageQuery, monumentQuery)
  }

  def specialNominations(contest: Contest, imageQuery: ImageQuery, monumentQuery: MonumentQuery) {
    val monumentsMap = SpecialNomination.nominations.map { nomination =>
      val monuments = monumentQuery.byPage(nomination.pages.head, nomination.listTemplate)
      (nomination, monuments)
    }.toMap

    val allMonuments = monumentsMap.values.flatMap(identity)

    ImageDB.create(contest, imageQuery, new MonumentDB(contest, allMonuments.toSeq)).map { imageDb =>

      val imageDbs: Map[SpecialNomination, ImageDB] = SpecialNomination.nominations.map { nomination =>
        (nomination, imageDb.subSet(monumentsMap(nomination)))
      }.toMap

      val output = new Output()
      val stat = output.specialNomination(imageDbs)

      println(stat)
    }
  }

  def regionalStat(wlmContest: Contest, allContests: Seq[Contest], monumentDb: MonumentDB, imageQuery: ImageQuery) {

    val dbsByYear = allContests.map(contest => ImageDB.create(contest, imageQuery, monumentDb))
//    val total = imageQuery.imagesWithTemplateAsync(wlmContest.fileTemplate, wlmContest)

    for {
      imageDbs <- Future.sequence(dbsByYear)
     // totalImages <- total
    } {

      initImages()
      for (imageDb <- imageDbs) {
        saveImages(imageDb)
      }

//      val totalImageDb = new ImageDB(wlmContest, totalImages, monumentDb)

//      val output = new Output()
//
//      val idsStat = output.monumentsPictured(imageDbs, totalImageDb, monumentDb)
//      println(idsStat)
//
//      val authorStat = output.authorsContributed(imageDbs, totalImageDb, monumentDb)
//      println(authorStat)
//
//      val toc = "__TOC__\n"
//      val category = "\n[[Category:Wiki Loves Monuments 2014 in Ukraine]]"
//      val regionalStat = toc + idsStat + authorStat + category
//
////      val bot = MwBot.get(MwBot.commons)
////      bot.await(bot.page("Commons:Wiki Loves Monuments 2014 in Ukraine/Regional statistics").edit(regionalStat, "update statistics"))
//
//      MwBot.get(MwBot.commons).getJavaWiki.edit("Commons:Wiki Loves Monuments 2014 in Ukraine/Regional statistics", regionalStat, "updating")
    }

  }

  def saveMonuments(monumentDb: MonumentDB) {
    import scala.slick.driver.H2Driver.simple._

    slick.db.withSession { implicit  session =>
      slick.monuments.ddl.drop
      slick.monuments.ddl.create

      slick.monuments ++= monumentDb.allMonuments
    }
  }

  def initImages(){

    slick.db.withSession { implicit  session =>
//      slick.images.ddl.drop
//      slick.images.ddl.create
    }
  }

  def saveImages(imageDb: ImageDB) {
    import scala.slick.driver.H2Driver.simple._

    slick.db.withSession { implicit  session =>
      slick.images ++= imageDb.images
    }
  }
}

object Statistics {
  def main(args: Array[String]) {
    new Statistics().init()
  }
}
