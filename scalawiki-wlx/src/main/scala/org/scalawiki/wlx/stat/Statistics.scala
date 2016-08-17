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
                       currentYearImageDb: ImageDB,
                       totalImageDb: Option[ImageDB],
                       dbsByYear: Seq[ImageDB] = Seq.empty,
                       monumentDbOld: Option[MonumentDB]  = None
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
      yield ContestStat(contest, startYear.getOrElse(contest.year), monumentDb, imageDB, totalImages, byYear, monumentDbOld)
  }

  def init(): Unit = {
    gatherData().map {
      data =>
        currentYear(data.contest, data.currentYearImageDb, data)

        for (totalImageDb <- data.totalImageDb) {
          regionalStat(data.contest, data.dbsByYear, data.currentYearImageDb, totalImageDb, data)
        }
    }
  }

  def articleStatistics(monumentDb: MonumentDB) = {
    println(Stats.withArticles(monumentDb).asWiki("Article Statistics").asWiki)
  }

  def toMassMessage(users: Iterable[String]) = {
    users.map(name => s"{{#target:User talk:$name}}")
  }

  def currentYear(contest: Contest, imageDb: ImageDB, stat: ContestStat) = {

    new SpecialNominations(contest, imageDb).specialNominations()

    new AuthorsStat().authorsStat(imageDb, bot)
    lessThan2MpGallery(contest, imageDb)

    imageDb.monumentDb.foreach {
      mDb =>
        wrongIds(imageDb, mDb)

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

  def wrongIds(imageDb: ImageDB, monumentDb: MonumentDB) {

    val wrongIdImages = imageDb.images.filterNot(image => image.monumentId.fold(false)(monumentDb.ids.contains))

    val contest = imageDb.contest
    val contestPage = contest.name

    val text = wrongIdImages.map(_.title).mkString("<gallery>", "\n", "</gallery>")
    bot.page(s"Commons:$contestPage/Images with bad ids").edit(text, Some("updating"))
  }

  def regionalStat(wlmContest: Contest,
                   imageDbs: Seq[ImageDB],
                   currentYear: ImageDB,
                   totalImageDb: ImageDB,
                   stat: ContestStat) {

    val contest = currentYear.contest
    val categoryName = contest.contestType.name + " in " + contest.country.name
    val monumentDb = currentYear.monumentDb

    val authorsStat = new AuthorsStat()

    val idsStat = monumentDb.map(db => new MonumentsPicturedByRegion(stat, uploadImages = true)).getOrElse("")

    val authorsContributed = authorsStat.authorsContributed(imageDbs, Some(totalImageDb), monumentDb)

    val toc = "__TOC__"
    val category = s"\n[[Category:$categoryName]]"
    val regionalStat = toc + idsStat + authorsContributed + category

    bot.page(s"Commons:$categoryName/Regional statistics").edit(regionalStat, Some("updating"))

    updateReport(new AuthorMonuments(currentYear))

    monumentDb.map (_ => updateReport(new MostPopularMonuments(stat)))
  }

  def updateReport(reporter: Reporter) = {
    bot.page(reporter.page).edit(reporter.asText, Some("updating"))
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
