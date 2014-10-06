package client.wlx

import client.MwBot
import client.slick.Slick
import client.wlx.dto.{Monument, Contest, SpecialNomination}
import client.wlx.query.{ImageQuery, ImageQueryApi, MonumentQuery}

import scala.collection.immutable.SortedSet

class Statistics {

  import scala.concurrent.ExecutionContext.Implicits.global

  val slick = new Slick()

  val wlmContest = Contest.WLMUkraine(2014, "09-15", "10-15")
  val previousContests = (2012 to 2013).map(year => Contest.WLMUkraine(year, "09-01", "09-30"))

  def init(): Unit = {

    val monumentQuery = MonumentQuery.create(wlmContest)
    val allMonuments = monumentQuery.byMonumentTemplate(wlmContest.listTemplate)

    val libraries = allMonuments.filter(m => m.name.toLowerCase.contains("бібліо") || m.place.fold(false)(_.contains("бібліо")))
    val monumentDb = new MonumentDB(wlmContest, allMonuments)

    //    val grouped: Map[String, Seq[Monument]] = monumentDb.wrongIdMonuments.groupBy(_.pageParam)
    //    for ((page, monuments)  <- grouped) {
    //      println(page)
    //      for (monument <- monuments)
    //      println(s"  id: ${monument.id}, name: ${monument.name},"
    //+s" place ${monument.place}"
    //      )
    //    }

    //    saveMonuments(monumentDb)
    //   new Output().monumentsByType(monumentDb)

    imagesStatistics(monumentQuery, monumentDb)
  }

  def imagesStatistics(monumentQuery: MonumentQuery, monumentDb: MonumentDB) {
    val imageQueryDb = ImageQuery.create(db = true)
    val imageQueryApi = ImageQuery.create(db = false)

    for (imageDb <- ImageDB.create(wlmContest, imageQueryApi, monumentDb)) {

      authorsStat(monumentDb, imageDb)
    //  byDayAndRegion(imageDb)
      specialNominations(wlmContest, imageDb, monumentQuery)
     // fixWadco(imageDb, monumentDb)

      val total = new ImageQueryApi().imagesWithTemplateAsync(wlmContest.fileTemplate, wlmContest)
      for (totalImages <- total) {

        val totalImageDb = new ImageDB(wlmContest, totalImages, monumentDb)

//        val withWrongIdsKh  = totalImages.filter(_.monumentId.exists(id => id.startsWith("63-101-") && !monumentDb.ids.contains(id)))
//        val text = withWrongIdsKh.map(image => s"${image.title}|${image.monumentId.getOrElse("")}").mkString("<gallery>", "\n", "</gallery>")
//        MwBot.get(MwBot.commons).page("User:IlyaBot/KharkivBadIdImages").edit(text, "updating")

        regionalStat(wlmContest, monumentDb, imageQueryDb, imageDb, totalImageDb)
      //  fillLists(monumentDb, totalImageDb)

        val badImages = totalImageDb.subSet(monumentDb.wrongIdMonuments, true)
        val byId = badImages._byId
      }
    }
  }

  def fixWadco(imageDb: ImageDB, monumentDb: MonumentDB) {
    val images = imageDb.byId("99-999-9999")
    for (image <- images) {
      val title = image.title
      if (title.size>=11) {
        val id = title.substring(0, 11)
        for (monument <-  monumentDb.byId(id)) {
         // image.
        }
      }
    }
  }

  def byDayAndRegion(imageDb: ImageDB): Unit = {

    val byDay = imageDb.withCorrectIds.groupBy(_.date.getOrElse("2014-00-00").substring(8, 10))

    val firstSlice = (16 to 16).flatMap(day => byDay.getOrElse(day.toString, Seq.empty))

    val byRegion = firstSlice.groupBy(im => Monument.getRegionId(im.monumentId))

    var text = ""
    val dayPage = "Commons:Wiki Loves Monuments 2014 in Ukraine/Day 2"
    for (regionId <- SortedSet(byRegion.keySet.toSeq:_*)) {
      val regionName: String = imageDb.monumentDb.contest.country.regionById(regionId).name
      val pageName = s"$dayPage Region $regionName"
      val gallery = byRegion(regionId).map(_.title).mkString("<gallery>\n", "\n", "</gallery>")

      text += s"* [[$pageName|$regionName]]\n"

      MwBot.get(MwBot.commons).page(pageName).edit(gallery, "updating")
    }
    MwBot.get(MwBot.commons).page(dayPage).edit(text, "updating")
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
            //            println(idsStat)

            val authorStat = output.authorsContributed(imageDbs, totalImageDb, monumentDb)
            //            println(authorStat)

            val toc = "__TOC__"
            val category = "\n[[Category:Wiki Loves Monuments 2014 in Ukraine]]"
            val regionalStat = toc + idsStat + authorStat + category

            //      val bot = MwBot.get(MwBot.commons)
            //      bot.await(bot.page("Commons:Wiki Loves Monuments 2014 in Ukraine/Regional statistics").edit(regionalStat, "update statistics"))

            MwBot.get(MwBot.commons).page("Commons:Wiki Loves Monuments 2014 in Ukraine/Regional statistics").edit(regionalStat, "updating")

            val authorsByRegionTotal = output.authorsMonuments(totalImageDb) + "\n[[Category:Wiki Loves Monuments in Ukraine]]"

            MwBot.get(MwBot.commons).page("Commons:Wiki Loves Monuments in Ukraine/3 years total number of objects pictured by uploader").edit(authorsByRegionTotal, "updating")

            val mostPopularMonuments  = output.mostPopularMonuments(imageDbs, totalImageDb, monumentDb)
            MwBot.get(MwBot.commons).page("Commons:Wiki Loves Monuments in Ukraine/Most photographed objects").edit(mostPopularMonuments, "updating")

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
