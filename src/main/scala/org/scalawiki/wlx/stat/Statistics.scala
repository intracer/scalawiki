package org.scalawiki.wlx.stat

import java.nio.file.{Files, Paths}

import org.scalawiki.MwBot
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.query.{ImageQuery, ImageQueryApi, MonumentQuery}
import org.scalawiki.wlx.{ImageDB, MonumentDB}

import scala.util.control.NonFatal

class Statistics {

  import scala.concurrent.ExecutionContext.Implicits.global

  val previousContests = (2012 to 2014).map(year => Contest.WLMUkraine(year, "05-01", "05-31"))

  def init(contest: Contest): Unit = {

    val monumentQuery = MonumentQuery.create(contest)

    val monumentDb = getMonumentDb(contest)

    //articleStatistics(monumentDb)
    imagesStatistics(monumentQuery, monumentDb)
  }

  def getMonumentDb(contest: Contest): MonumentDB = {
    val monumentQuery = MonumentQuery.create(contest)
    var allMonuments = monumentQuery.byMonumentTemplate(contest.uploadConfigs.head.listTemplate)

    if (contest.country.code == "ru") {
      allMonuments = allMonuments.filter(_.page.contains("Природные памятники России"))
    }

    new MonumentDB(contest, allMonuments)
  }

  def articleStatistics(monumentDb: MonumentDB) = {
    println(Stats.withArticles(monumentDb).asWiki("Article Statistics"))
  }

  def imagesStatistics(monumentQuery: MonumentQuery, monumentDb: MonumentDB) {
    val imageQueryDb = ImageQuery.create(db = false)
    val imageQueryApi = ImageQuery.create(db = false)

    val contest = monumentDb.contest
    val imageDbFuture = ImageDB.create(contest, imageQueryApi, monumentDb)

    imageDbFuture onFailure {
      case f =>
        println("Failure " + f)
    }

    imageDbFuture onSuccess {
      case imageDb =>

        //        val authors = imageDb.authors.toSeq
        //        authors.foreach(user => message(MwBot.get(MwBot.commons), user, message1))

        authorsStat(monumentDb, imageDb)
        //        byDayAndRegion(imageDb)
        //        new SpecialNominations().specialNominations(contest, imageDb, monumentQuery)
        wrongIds(contest, imageDb, monumentDb)

        lessThan2MpGallery(contest, imageDb)

        fillLists(monumentDb, imageDb)

        val total = new ImageQueryApi().imagesWithTemplateAsync(contest.uploadConfigs.head.fileTemplate, contest)
        for (totalImages <- total) {

          val totalImageDb = new ImageDB(contest, totalImages, monumentDb)

          regionalStat(contest, monumentDb, imageQueryDb, imageDb, totalImageDb)

          Thread.sleep(5000)
        }
    }
  }

  def message(bot: MwBot, user: String, msg: String => String): Unit = {
    bot.page("User_talk:" + user).edit(msg(user), section = Some("new"))
  }

  def lessThan2MpGallery(contest: Contest, imageDb: ImageDB) = {
    val lessThan2Mp = imageDb.byMegaPixelFilterAuthorMap(_ < 2)
    val gallery = new Output().authorsImages(lessThan2Mp, imageDb.monumentDb)
    val contestPage = s"${contest.contestType.name} ${contest.year} in ${contest.country.name}"

    MwBot.get(MwBot.commons).page(s"Commons:$contestPage/Less than 2Mp").edit(gallery, Some("updating"))
  }

  def wrongIds(wlmContest: Contest, imageDb: ImageDB, monumentDb: MonumentDB) {

    val wrongIdImages = imageDb.images.filterNot(image => image.monumentId.fold(false)(monumentDb.ids.contains))

    val contest = imageDb.contest
    val contestPage = s"${contest.contestType.name} ${contest.year} in ${contest.country.name}"

    val text = wrongIdImages.map(_.title).mkString("<gallery>", "\n", "</gallery>")
    MwBot.get(MwBot.commons).page(s"Commons:$contestPage/Images with bad ids").edit(text, Some("updating"))
  }

  def byDayAndRegion(imageDb: ImageDB): Unit = {

    //    val byDay = imageDb.withCorrectIds.groupBy(_.date.map(_.toDate))
    //
    //    val firstSlice = (16 to 16).flatMap(day => byDay.get)//.getOrElse(day.toString, Seq.empty))
    //
    //    val byRegion = firstSlice.groupBy(im => Monument.getRegionId(im.monumentId))
    //
    //    var text = ""
    //
    //    val contest = imageDb.contest
    //    val contestPage = s"${contest.contestType.name} ${contest.year} in ${contest.country.name}"
    //
    //    val dayPage = s"Commons:$contestPage/Day 2"
    //    for (regionId <- SortedSet(byRegion.keySet.toSeq: _*)) {
    //      val regionName: String = imageDb.monumentDb.contest.country.regionById(regionId).name
    //      val pageName = s"$dayPage Region $regionName"
    //      val gallery = byRegion(regionId).map(_.title).mkString("<gallery>\n", "\n", "</gallery>")
    //
    //      text += s"* [[$pageName|$regionName]]\n"
    //
    //      MwBot.get(MwBot.commons).page(pageName).edit(gallery, "updating")
    //    }
    //    MwBot.get(MwBot.commons).page(dayPage).edit(text, "updating")
  }

  def authorsStat(monumentDb: MonumentDB, imageDb: ImageDB) {
    val output = new Output()
    val text = output.authorsMonuments(imageDb)
    Files.write(Paths.get("authorsRating.txt"), text.getBytes)

    val contest = imageDb.contest
    val contestPage = s"${contest.contestType.name} ${contest.year} in ${contest.country.name}"

    MwBot.get(MwBot.commons).page(s"Commons:$contestPage/Number of objects pictured by uploader").edit(text, Some("updating"))
  }

  def regionalStat(wlmContest: Contest, monumentDb: MonumentDB,
                   imageQueryDb: ImageQuery,
                   currentYear: ImageDB,
                   totalImageDb: ImageDB) {

    val contest = monumentDb.contest
    val categoryName = contest.contestType.name + " in " + contest.country.name


    val dbsByYear = previousContests.map(contest => ImageDB.create(contest, imageQueryDb, monumentDb))

    dbsByYear.head.map {
      firstYear =>
        dbsByYear.last.map {
          lastYear =>

            try {
              val imageDbs = Seq(firstYear, lastYear, currentYear)

              val output = new Output()

              val idsStat = output.monumentsPictured(imageDbs, totalImageDb, monumentDb)
              println(idsStat)

              val authorStat = output.authorsContributed(imageDbs, totalImageDb, monumentDb)
              //            println(authorStat)

              val toc = "__TOC__"
              val category = s"\n[[Category:$categoryName]]"
              val regionalStat = toc + idsStat + authorStat + category

              MwBot.get(MwBot.commons).page(s"Commons:$categoryName/Regional statistics").edit(regionalStat, Some("updating"))

              val authorsByRegionTotal = output.authorsMonuments(totalImageDb) + s"\n[[Category:$categoryName]]"

              MwBot.get(MwBot.commons).page(s"Commons:$categoryName/3 years total number of objects pictured by uploader").edit(authorsByRegionTotal, Some("updating"))

              val mostPopularMonuments = output.mostPopularMonuments(imageDbs, totalImageDb, monumentDb)
              MwBot.get(MwBot.commons).page(s"Commons:$categoryName/Most photographed objects").edit(mostPopularMonuments, Some("updating"))

              val monumentQuery = MonumentQuery.create(wlmContest)

              //              new SpecialNominations().specialNominations(previousContests.find(_.year == 2013).get, firstYear, monumentQuery)
              //              new SpecialNominations().specialNominations(previousContests.find(_.year == 2014).get, lastYear, monumentQuery)
            }
            catch {
              case NonFatal(e) =>
                println(e)
                e.printStackTrace()
                throw e
            }
        }
    }
  }


  def fillLists(monumentDb: MonumentDB, imageDb: ImageDB): Unit = {
    //ListFiller.fillLists(monumentDb, imageDb)
  }

}

object Statistics {
  def main(args: Array[String]) {

    //    Kamon.start()

    val stat = new Statistics()

    stat.init(Contest.WLMUkraine(2015, "05-01", "05-31"))

    //stat.message(MwBot.get(MwBot.commons), "Ilya", stat.message1)

    //    val contests = Contest.allWLE
    //    val dbs = contests.map(stat.getMonumentDb)
    //
    //    println(new MonumentDbStat().getStat(dbs))

  }
}
