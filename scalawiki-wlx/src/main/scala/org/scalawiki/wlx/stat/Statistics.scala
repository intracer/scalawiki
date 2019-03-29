package org.scalawiki.wlx.stat

import java.time.{ZoneOffset, ZonedDateTime}

import org.scalawiki.MwBot
import org.scalawiki.cache.CachedBot
import org.scalawiki.dto.Site
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.query.{ImageQuery, MonumentQuery}
import org.scalawiki.wlx.{ImageDB, ListFiller, MonumentDB}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Holds fetched contest data
  *
  * @param contest            contest: contest type (WLM/WLE), country, year, etc.
  * @param startYear          the first year contest was held, or the first year that we are interested in
  * @param monumentDb         cultural/natural monuments database for the contest
  * @param currentYearImageDb image database for current year's contest
  * @param totalImageDb       image database that holds images of all monuments from the contest, regardless of when they where uploaded
  * @param dbsByYear          image databases split by contest year
  * @param monumentDbOld      monument database at current year's contest start. Used to rate users who submitted newly pictured monuments.
  */
case class ContestStat(contest: Contest,
                       startYear: Int,
                       monumentDb: Option[MonumentDB],
                       currentYearImageDb: Option[ImageDB],
                       totalImageDb: Option[ImageDB],
                       dbsByYear: Seq[ImageDB] = Seq.empty,
                       monumentDbOld: Option[MonumentDB] = None)

/**
  * Coordinates fetching contest statistics and creating reports/galleries etc. Needs refactoring.
  *
  * @param contest       contest: contest type (WLM/WLE), country, year, etc.
  * @param startYear     the first year contest was held, or the first year that we are interested in
  * @param monumentQuery monuments fetcher
  * @param imageQuery    images fetcher
  * @param bot           scalawiki bot instance
  */
class Statistics(contest: Contest,
                 startYear: Option[Int],
                 monumentQuery: MonumentQuery,
                 imageQuery: ImageQuery,
                 imageQueryWiki: Option[ImageQuery],
                 bot: MwBot,
                 cfg: StatConfig) {

  def this(contest: Contest,
           startYear: Option[Int] = None,
           monumentQuery: MonumentQuery,
           imageQuery: ImageQuery = ImageQuery.create(),
           imageQueryWiki: Option[ImageQuery] = None,
           bot: MwBot = MwBot.fromHost(MwBot.commons),
           cfg: Option[StatConfig] = None) =
    this(contest, startYear, monumentQuery, imageQuery, imageQueryWiki, bot, cfg.getOrElse(StatConfig(contest.campaign)))

  val currentYear = contest.year

  val contests = (startYear.getOrElse(currentYear) to currentYear).map(y => contest.copy(year = y))

  /**
    * Fetches contest data
    *
    * @param total whether to fetch image database that holds images of all monuments from the contest, regardless of when they where uploaded
    * @return asynchronously returned contest data
    */
  def gatherData(total: Boolean): Future[ContestStat] = {

    val (monumentDb, monumentDbOld) = (
      Some(MonumentDB.getMonumentDb(contest, monumentQuery)),
      contest.newObjectRating.map { _ =>
        MonumentDB.getMonumentDb(contest, monumentQuery, date = Some(ZonedDateTime.of(2017, 4, 30, 23, 59, 0, 0, ZoneOffset.UTC)))
      }
    )

    for (byYear <- Future.sequence(contests.map(contestImages(monumentDb)));
         totalImages <- if (total) imagesByTemplate(monumentDb) else Future.successful(None)
    ) yield {
      val currentYearImages = byYear.find(_.contest.year == currentYear)

      val mDbOld: Option[MonumentDB] = currentYearImages.flatMap(getOldImagesMonumentDb(monumentDb, monumentDbOld, totalImages, _))

      ContestStat(contest, startYear.getOrElse(contest.year), monumentDb, currentYearImages, totalImages, byYear, mDbOld)
    }
  }

  private def contestImages(monumentDb: Some[MonumentDB])(contest: Contest) =
    ImageDB.create(contest, imageQuery, monumentDb)

  private def imagesByTemplate(monumentDb: Some[MonumentDB], imageQuery: ImageQuery = imageQuery) =
    for (commons <- imageQuery.imagesWithTemplateAsync(contest.uploadConfigs.head.fileTemplate, contest);
         wiki <- imageQueryWiki.map(_.imagesWithTemplateAsync(contest.uploadConfigs.head.fileTemplate, contest))
           .getOrElse(Future.successful(Nil))) yield {
      Some(new ImageDB(contest, commons ++ wiki, monumentDb))
    }

  def getOldImagesMonumentDb(monumentDb: Option[MonumentDB], monumentDbOld: Option[MonumentDB],
                             totalImages: Option[ImageDB], imageDB: ImageDB): Option[MonumentDB] = {
    for (mDb <- monumentDb;
         mdbOld <- monumentDbOld;
         total <- totalImages.orElse(Some(new ImageDB(contest, Seq.empty)))) yield {
      val oldIds = mdbOld.monuments.filter(_.photo.isDefined).map(_.id).toSet ++
        total.images.filterNot(ti => imageDB.images.exists(i => i.pageId.exists(ti.pageId.contains))).flatMap(_.monumentId)

      new MonumentDB(contest, mDb.monuments.filter(m => oldIds.contains(m.id)))
    }
  }

  def init(total: Boolean): Unit = {
    gatherData(total = total).map { data =>
        data.currentYearImageDb.foreach(imageDb => currentYear(data.contest, imageDb, data))
        for (totalImageDb <- data.totalImageDb) {
          regionalStat(data.contest, data.dbsByYear, totalImageDb, data)

          new AuthorsStat().authorsStat(data, bot, cfg.gallery)
        }
    }.failed.map(println)
  }

  def articleStatistics(monumentDb: MonumentDB) = {
    println(Stats.withArticles(monumentDb).asWiki("Article Statistics").asWiki)
  }

  def toMassMessage(users: Iterable[String]) = {
    users.map(name => s"{{#target:User talk:$name}}")
  }

  /**
    * Outputs current year reports.
    *
    * @param contest
    * @param imageDb
    * @param stat
    */
  def currentYear(contest: Contest, imageDb: ImageDB, stat: ContestStat) = {

    //new SpecialNominations(contest, imageDb).specialNominations()

    lessThan2MpGallery(contest, imageDb)

    imageDb.monumentDb.foreach {
      mDb =>
        wrongIds(imageDb, mDb)

      //fillLists(mDb, imageDb)
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

    val wrongIdImages = imageDb.images
      .filterNot(image => image.monumentId.fold(false)(id => monumentDb.ids.contains(id) || id.startsWith("99")))

    val notObvious = wrongIdImages.filterNot(_.categories.exists(_.startsWith("Obviously ineligible")))

    val contest = imageDb.contest
    val contestPage = contest.name

    val text = notObvious.map(_.title).mkString("<gallery>", "\n", "</gallery>")
    bot.page(s"Commons:$contestPage/Images with bad ids").edit(text, Some("updating"))
  }

  def regionalStat(wlmContest: Contest,
                   imageDbs: Seq[ImageDB],
                   totalImageDb: ImageDB,
                   stat: ContestStat) {

    val contest = totalImageDb.contest
    val categoryName = contest.contestType.name + " in " + contest.country.name
    val monumentDb = totalImageDb.monumentDb

    val authorsStat = new AuthorsStat()

    val idsStat = monumentDb.map(_ => new MonumentsPicturedByRegion(stat, uploadImages = true).asText).getOrElse("")

    val authorsContributed = authorsStat.authorsContributed(imageDbs, Some(totalImageDb), monumentDb)

    val toc = "__TOC__"
    val category = s"\n[[Category:$categoryName]]"
    val regionalStat = toc + idsStat + authorsContributed + category

    bot.page(s"Commons:$categoryName/Regional statistics").edit(regionalStat, Some("updating"))

    monumentDb.map(_ => new MostPopularMonuments(stat).updateWiki(bot))
  }

  def fillLists(monumentDb: MonumentDB, imageDb: ImageDB): Unit = {
    ListFiller.fillLists(monumentDb, imageDb)
  }
}


object Statistics {


  def main(args: Array[String]) {
    val cfg = StatParams.parse(args)

    val contest = Contest.byCampaign(cfg.campaign).get
      .copy(
        year = cfg.years.last,
        newObjectRating = cfg.newObjectRating,
        newAuthorObjectRating = cfg.newAuthorObjectRating
      )

    val cacheName = s"${cfg.campaign}-${contest.year}"
    val imageQuery = ImageQuery.create()(new CachedBot(Site.commons, cacheName, true))
    val imageQueryWiki = ImageQuery.create()(new CachedBot(Site.ukWiki, cacheName + "-wiki", true))

    val stat = new Statistics(
      contest,
      startYear = Some(cfg.years.head),
      monumentQuery = MonumentQuery.create(contest),
      cfg = Some(cfg),
      imageQuery = imageQuery,
      imageQueryWiki = Some(imageQueryWiki)
    )

    stat.init(total = cfg.years.size > 1)
  }
}
