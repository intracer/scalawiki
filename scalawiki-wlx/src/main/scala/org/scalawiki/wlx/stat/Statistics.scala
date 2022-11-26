package org.scalawiki.wlx.stat

import java.time.{ZoneOffset, ZonedDateTime}
import org.scalawiki.MwBot
import org.scalawiki.cache.CachedBot
import org.scalawiki.dto.{Image, Site}
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.query.{ImageQuery, MonumentQuery}
import org.scalawiki.wlx.stat.reports.ReporterRegistry
import org.scalawiki.wlx.{ImageDB, MonumentDB}

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
                       monumentDb: Option[MonumentDB] = None,
                       currentYearImageDb: Option[ImageDB] = None,
                       totalImageDb: Option[ImageDB] = None,
                       dbsByYear: Seq[ImageDB] = Seq.empty,
                       monumentDbOld: Option[MonumentDB] = None,
                       config: Option[StatConfig] = None) {

  val imageDbsByYear = dbsByYear.groupBy(_.contest.year)
  val yearSeq = imageDbsByYear.keys.toSeq.sorted

  lazy val oldImages: Iterable[Image] = {
    for (total <- totalImageDb;
    current <- currentYearImageDb)
      yield {
        val currentImageIds = current.images.flatMap(_.pageId).toSet
        total.images.filter(image => !currentImageIds.contains(image.pageId.get))
      }
  }.getOrElse(Nil)

  def imageDbByYear(year: Int) = imageDbsByYear.get(year).map(_.head)

  def mapYears[T](f: ImageDB => T) = {
    for (year <- yearSeq;
         imageDb <- imageDbByYear(year))
      yield f(imageDb)
  }
}

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
                 config: StatConfig) {

  def this(contest: Contest,
           startYear: Option[Int] = None,
           monumentQuery: MonumentQuery,
           imageQuery: ImageQuery = ImageQuery.create(),
           imageQueryWiki: Option[ImageQuery] = None,
           bot: MwBot = MwBot.fromHost(MwBot.commons),
           config: Option[StatConfig] = None) =
    this(contest, startYear, monumentQuery, imageQuery, imageQueryWiki, bot, config.getOrElse(StatConfig(contest.campaign)))

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
      contest.rateConfig.newObjectRating.map { _ =>
        MonumentDB.getMonumentDb(contest, monumentQuery, date = Some(ZonedDateTime.of(2017, 4, 30, 23, 59, 0, 0, ZoneOffset.UTC)))
      }
    )

    for (byYear <- Future.sequence(contests.map(contestImages(monumentDb)));
         totalImages <- if (total) imagesByTemplate(monumentDb)
         else Future.successful(Some(new ImageDB(contest, byYear.find(_.contest.year == currentYear).map(_.images).getOrElse(Nil), monumentDb)))
    ) yield {
      val currentYearImages = byYear.find(_.contest.year == currentYear)

      val mDbOld: Option[MonumentDB] = currentYearImages.flatMap(getOldImagesMonumentDb(monumentDb, monumentDbOld, totalImages, _))

      ContestStat(contest, startYear.getOrElse(contest.year), monumentDb, currentYearImages, totalImages, byYear, mDbOld, Some(config))
    }
  }

  private def contestImages(monumentDb: Some[MonumentDB])(contest: Contest) =
    ImageDB.create(contest, imageQuery, monumentDb, config.minMpx)

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
    gatherData(total = total).map { stat =>
      new ReporterRegistry(stat, config).output()
    }.failed.map(println)
  }

  def articleStatistics(monumentDb: MonumentDB) = {
    println(Stats.withArticles(monumentDb).asWiki("Article Statistics").asWiki)
  }

  def toMassMessage(users: Iterable[String]) = {
    users.map(name => s"{{#target:User talk:$name}}")
  }

  def message(bot: MwBot, user: String, msg: String => String): Unit = {
    bot.page("User_talk:" + user).edit(msg(user), section = Some("new"))
  }
}

object Statistics {

  def getContest(cfg: StatConfig): Contest = {
    val contest = Contest.byCampaign(cfg.campaign).getOrElse {
      throw new IllegalArgumentException(s"Unknown campaign: ${cfg.campaign}")
    }

    contest.copy(
      year = cfg.years.last,
      rateConfig = cfg.rateConfig
    )
  }

  def main(args: Array[String]) {
    val cfg = StatParams.parse(args)
    val contest = getContest(cfg)

    val cacheName = s"${cfg.campaign}-${contest.year}"
    val imageQuery = ImageQuery.create()(new CachedBot(Site.commons, cacheName, true))
    val imageQueryWiki = ImageQuery.create()(new CachedBot(Site.ukWiki, cacheName + "-wiki", true, entries = 100))

    val stat = new Statistics(
      contest,
      startYear = Some(cfg.years.head),
      monumentQuery = MonumentQuery.create(contest, reportDifferentRegionIds = true),
      config = Some(cfg),
      imageQuery = imageQuery,
      imageQueryWiki = Some(imageQueryWiki)
    )

    stat.init(total = cfg.years.size > 1)
  }
}
