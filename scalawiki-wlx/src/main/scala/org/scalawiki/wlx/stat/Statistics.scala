package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.query.{ImageQuery, ImageQueryApi, MonumentQuery}
import org.scalawiki.wlx.{ImageDB, ListFiller, MonumentDB}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class ContestStat(contest: Contest,
                       startYear: Int,
                       monumentDb: Option[MonumentDB],
                       monumentDbOld: Option[MonumentDB],
                       currentYearImageDb: ImageDB,
                       totalImageDb: Option[ImageDB],
                       dbsByYear: Seq[ImageDB] = Seq.empty
                      )

class Statistics(contest: Contest,
                 startYear: Option[Int] = None,
                 monumentQuery: MonumentQuery,
                 imageQuery: ImageQuery = ImageQuery.create(),
                 bot: MwBot = MwBot.fromHost(MwBot.commons)
                ) {

  val currentYear = contest.year

  val previousContests = startYear.fold(Seq.empty[Contest]) { year =>
    (year until currentYear).map(year => contest.copy(year = year))
  }

  def gatherData(total: Boolean = false, byYear: Boolean = false): Future[ContestStat] = {

    val (monumentDb, monumentDbOld) = (Some(MonumentDB.getMonumentDb(contest, monumentQuery)), None)

    val imageDbFuture = ImageDB.create(contest, imageQuery, monumentDb, monumentDbOld)

    val totalFuture = if (total)
      new ImageQueryApi().imagesWithTemplateAsync(contest.uploadConfigs.head.fileTemplate, contest).map {
        images =>
          Some(new ImageDB(contest, images, monumentDb))
      } else Future.successful(None)

    val byYearFuture = if (byYear)
      Future.sequence(previousContests.map(contest => ImageDB.create(contest, imageQuery, monumentDb)) ++ Seq(imageDbFuture))
    else Future.successful(Seq.empty)

    for (imageDB <- imageDbFuture;
         totalImages <- totalFuture;
         byYear <- byYearFuture)
      yield ContestStat(contest, startYear.getOrElse(contest.year), monumentDb, monumentDbOld, imageDB, totalImages, byYear)
  }

  def init(): Unit = {
    gatherData().map {
      data =>
        currentYear(data.contest, data.currentYearImageDb)

        for (totalImageDb <- data.totalImageDb) {
          regionalStat(data.contest, data.dbsByYear, data.currentYearImageDb, totalImageDb)
        }
    }
  }

  def articleStatistics(monumentDb: MonumentDB) = {
    println(Stats.withArticles(monumentDb).asWiki("Article Statistics").asWiki)
  }

  def toMassMessage(users: Iterable[String]) = {
    users.map(name => s"{{#target:User talk:$name}}")
  }

  def currentYear(contest: Contest, imageDb: ImageDB) = {

    new SpecialNominations().specialNominations(contest, imageDb)

    new AuthorsStat().authorsStat(imageDb, bot)
    byDayAndRegion(imageDb)
    lessThan2MpGallery(contest, imageDb)

    imageDb.monumentDb.foreach {
      mDb =>
        wrongIds(contest, imageDb, mDb)

        fillLists(mDb, imageDb)
    }
  }

  def message(bot: MwBot, user: String, msg: String => String): Unit = {
    bot.page("User_talk:" + user).edit(msg(user), section = Some("new"))
  }

  def lessThan2MpGallery(contest: Contest, imageDb: ImageDB) = {
    val lessThan2Mp = imageDb.byMegaPixelFilterAuthorMap(_ < 2)
    val gallery = new AuthorsStat().authorsImages(lessThan2Mp, imageDb.monumentDb)
    val contestPage = contest.name

    bot.page(s"Commons:$contestPage/Less than 2Mp").edit(gallery, Some("updating"))
  }

  def wrongIds(wlmContest: Contest, imageDb: ImageDB, monumentDb: MonumentDB) {

    val wrongIdImages = imageDb.images.filterNot(image => image.monumentId.fold(false)(monumentDb.ids.contains))

    val contest = imageDb.contest
    val contestPage = contest.name

    val text = wrongIdImages.map(_.title).mkString("<gallery>", "\n", "</gallery>")
    bot.page(s"Commons:$contestPage/Images with bad ids").edit(text, Some("updating"))
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

  def regionalStat(wlmContest: Contest,
                   imageDbs: Seq[ImageDB],
                   currentYear: ImageDB,
                   totalImageDb: ImageDB) {

    val contest = currentYear.contest
    val categoryName = contest.contestType.name + " in " + contest.country.name
    val monumentDb = currentYear.monumentDb

    val output = new Output()
    val authorsStat = new AuthorsStat()

    val idsStat = monumentDb.map(db => output.monumentsPictured(imageDbs, totalImageDb, db)).getOrElse("")

    val authorsContributed = authorsStat.authorsContributed(imageDbs, totalImageDb, monumentDb)

    val toc = "__TOC__"
    val category = s"\n[[Category:$categoryName]]"
    val regionalStat = toc + idsStat + authorsContributed + category

    bot.page(s"Commons:$categoryName/Regional statistics").edit(regionalStat, Some("updating"))

    val authorsByRegionTotal = authorsStat.authorsMonuments(totalImageDb) + s"\n[[Category:$categoryName]]"

    bot.page(s"Commons:$categoryName/Total number of objects pictured by uploader").edit(authorsByRegionTotal, Some("updating"))

    monumentDb.map {
      db =>
        val mostPopularMonuments = output.mostPopularMonuments(imageDbs, totalImageDb, db)
        bot.page(s"Commons:$categoryName/Most photographed objects").edit(mostPopularMonuments, Some("updating"))
    }
  }

  def fillLists(monumentDb: MonumentDB, imageDb: ImageDB): Unit = {
    ListFiller.fillLists(monumentDb, imageDb)
  }

  def photoWithoutArticle(imageDb: ImageDB): String = {

    val monumentDb = imageDb.monumentDb.get

    val all = monumentDb.monuments.filter(m =>
      m.photo.isDefined &&
        m.article.isEmpty && !m.name.contains("[[") &&
        m.types.map(_.toLowerCase).exists(_.contains("нац")
          && imageDb.authorsCountById.getOrElse(m.id, 0) > 1
          && imageDb.byId(m.id).size > 2)
    )
    val byRegion = all.groupBy(_.regionId)

    val perRegion = monumentDb.contest.country.regions.sortBy(_.name).map {
      region =>
        val regionHeader = s"== ${region.name} ==\n"

        val monuments = byRegion.getOrElse(region.code, Seq.empty)

        val images = monuments.map(_.photo.get)
        val descriptions = monuments.map(m => s"[[${m.name}]], ${m.city.getOrElse("")}")

        val gallery = Image.gallery(images, descriptions)

        regionHeader + gallery
    }

    perRegion.mkString("\n")
  }

}

object Statistics {
  def main(args: Array[String]) {

    val contest: Contest = Contest.WLEUkraine(2016, "05-01", "05-31")
    val stat = new Statistics(contest, monumentQuery = MonumentQuery.create(contest))

    stat.init()
  }
}
