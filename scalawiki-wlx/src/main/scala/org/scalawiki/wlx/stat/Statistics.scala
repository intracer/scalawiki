package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.cache.CachedBot
import org.scalawiki.dto.{Image, Site}
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.query.{ImageQuery, MonumentQuery}
import org.scalawiki.wlx.stat.reports.ReporterRegistry
import org.scalawiki.wlx.{ImageCsvExporter, ImageCsvImporter, ImageDB, MonumentDB}

import java.io.{File, FileNotFoundException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/** Holds fetched contest data
  *
  * @param contest
  *   contest: contest type (WLM/WLE), country, year, etc.
  * @param startYear
  *   the first year contest was held, or the first year that we are interested in
  * @param monumentDb
  *   cultural/natural monuments database for the contest
  * @param currentYearImageDb
  *   image database for current year's contest
  * @param totalImageDb
  *   image database that holds images of all monuments from the contest, regardless of when they
  *   where uploaded
  * @param dbsByYear
  *   image databases split by contest year
  */
case class ContestStat(
    contest: Contest,
    startYear: Int,
    monumentDb: Option[MonumentDB] = None,
    currentYearImageDb: ImageDB,
    totalImageDb: ImageDB,
    dbsByYear: Seq[ImageDB] = Nil,
    config: Option[StatConfig] = None
) {

  val imageDbsByYear: Map[Int, Seq[ImageDB]] = dbsByYear.groupBy(_.contest.year)
  val yearSeq: Seq[Int] = imageDbsByYear.keys.toSeq.sorted

  lazy val oldImages: Iterable[Image] = {
    val currentImageIds = currentYearImageDb.images.flatMap(_.pageId).toSet
    totalImageDb.images.filterNot(_.pageId.exists(currentImageIds.contains))
  }

  def imageDbByYear(year: Int): Option[ImageDB] = imageDbsByYear.get(year).map(_.head)

  def mapYears[T](f: ImageDB => T): Seq[T] =
    for {
      year <- yearSeq
      imageDb <- imageDbByYear(year)
    } yield f(imageDb)
}

/** Coordinates fetching contest statistics and creating reports/galleries etc. Needs refactoring.
  *
  * @param contest
  *   contest: contest type (WLM/WLE), country, year, etc.
  * @param startYear
  *   the first year contest was held, or the first year that we are interested in
  * @param monumentQuery
  *   monuments fetcher
  * @param imageQuery
  *   images fetcher
  * @param bot
  *   scalawiki bot instance
  */
class Statistics(
    contest: Contest,
    startYear: Option[Int],
    monumentQuery: MonumentQuery,
    imageQuery: Option[ImageQuery],
    imageQueryWiki: Option[ImageQuery],
    bot: MwBot,
    config: StatConfig
) {

  def this(
      contest: Contest,
      startYear: Option[Int] = None,
      monumentQuery: MonumentQuery,
      imageQuery: Option[ImageQuery] = Some(ImageQuery.create),
      imageQueryWiki: Option[ImageQuery] = None,
      bot: MwBot = MwBot.fromHost(MwBot.commons),
      config: Option[StatConfig] = None
  ) =
    this(
      contest,
      startYear,
      monumentQuery,
      imageQuery,
      imageQueryWiki,
      bot,
      config.getOrElse(StatConfig(contest.campaign))
    )

  private val currentYear = contest.year

  private val contests =
    (startYear.getOrElse(currentYear) to currentYear).map(y => contest.copy(year = y))

  private lazy val totalImageQuery: ImageQuery = imageQuery.getOrElse(getImageQuery())

  def getImageQuery(year: Option[Int] = None): ImageQuery = {
    val cacheName = s"${contest.campaign}-${year.getOrElse("all")}"
    ImageQuery.create(new CachedBot(Site.commons, cacheName, true))
  }

  /** Fetches contest data
    *
    * @param total
    *   whether to fetch image database that holds images of all monuments from the contest,
    *   regardless of when they where uploaded
    * @return
    *   asynchronously returned contest data
    */
  def gatherData(total: Boolean): Future[ContestStat] = {
    val monumentDb = Some(MonumentDB.getMonumentDb(contest, monumentQuery))

    val byYearFutures = contests.map(contestImages(monumentDb))
    val totalFromCsv =
      if (csvRefresh) None else totalCsvReadPath.filter(new File(_).exists())
    val totalPageIdsFuture =
      if (total && totalFromCsv.isEmpty) imageIdsByTemplate() else Future.successful(Nil)
    for {
      byYear <- Future.sequence(byYearFutures)
      currentYearImages = byYear.last
      totalPageIds <- totalPageIdsFuture
      totalImages <-
        if (!total) Future.successful(currentYearImages)
        else
          totalFromCsv match {
            case Some(path) =>
              Future.successful(
                new ImageDB(contest, ImageCsvImporter.imagesFromCsv(path), monumentDb, config.minMpx)
              )
            case None =>
              imagesByTemplate(monumentDb, byYear, totalPageIds).map { db =>
                writeTotalCsvCache(db)
                db
              }
          }
    } yield {
      ContestStat(
        contest,
        startYear.getOrElse(contest.year),
        monumentDb,
        currentYearImages,
        totalImages,
        byYear,
        Some(config)
      )
    }
  }

  // ---- image CSV cache -----------------------------------------------------
  //
  // A second-tier cache next to the ChronicleMap `.cache` files: once an
  // `ImageDB` has been built it is serialized to `<csvDir>/<campaign>-<year>-images.csv`
  // (and `<campaign>-all-images.csv` for the all-time DB). Later runs read those
  // CSVs directly and skip the sequential JSON parse of the ChronicleMap.
  //
  // - `--images-from-csv <dir>` keeps its strict semantics (files must exist);
  //   `--csv-cache-refresh` does not apply there (those files are user-managed
  //   via `--export-images-csv`).
  // - otherwise the cache lives under `csv-cache/` and is filled on demand.
  // - past contest years are frozen once written. The current contest year is
  //   incrementally synced (diff the category id list, fetch only new files) and
  //   its CSV is written to the same `<campaign>-<year>-images.csv` path that
  //   next year's run will read as the frozen past-year copy -- so the last
  //   mid-contest sync of year N becomes the permanent record of year N. Delete
  //   that CSV to force a full refetch (removing only the `.cache` does nothing,
  //   the CSV short-circuits before the ChronicleMap is consulted).
  // - `--csv-cache-refresh` ignores existing CSVs and overwrites them.

  private val csvStrictDir: Option[String] = config.imagesFromCsv
  private val csvAutoCache: Boolean = config.csvCache && csvStrictDir.isEmpty
  private val csvDir: String = config.effectiveCsvCacheDir
  private val csvRefresh: Boolean = config.csvCacheRefresh && csvAutoCache

  private lazy val liveImageQuery: ImageQuery = ImageQuery.create

  private def yearCsvPath(year: Int): String =
    ImageCsvExporter.filename(contest.campaign, year, isCurrent = false, csvDir)

  private def totalCsvReadPath: Option[String] =
    csvStrictDir
      .map(dir => ImageCsvExporter.totalFilename(contest.campaign, dir))
      .orElse(if (csvAutoCache) Some(ImageCsvExporter.totalFilename(contest.campaign, csvDir)) else None)

  private def writeCsvCache(imageDb: ImageDB): Unit =
    if (csvAutoCache)
      ImageCsvExporter.export(imageDb, contest.campaign, isCurrent = false, csvDir)

  private def writeTotalCsvCache(imageDb: ImageDB): Unit =
    if (csvAutoCache)
      ImageCsvExporter.exportTotal(imageDb, contest.campaign, csvDir)

  private def contestImages(monumentDb: Some[MonumentDB])(yearContest: Contest): Future[ImageDB] =
    if (yearContest.year != currentYear) pastYearImages(monumentDb)(yearContest)
    else currentYearImages(monumentDb)(yearContest)

  private def pastYearImages(monumentDb: Some[MonumentDB])(yearContest: Contest): Future[ImageDB] = {
    val year = yearContest.year
    pastYearCsv(year) match {
      case Some(images) =>
        Future.successful(new ImageDB(yearContest, images, monumentDb, config.minMpx))
      case None =>
        fetchImageDb(yearContest, monumentDb).map { db =>
          writeCsvCache(db)
          db
        }
    }
  }

  /** Images for a past contest year from CSV, when a CSV should be used. */
  private def pastYearCsv(year: Int): Option[Seq[Image]] = csvStrictDir match {
    case Some(_) => imagesFromCsvOpt(year) // strict: throws if the file is missing
    case None =>
      val path = yearCsvPath(year)
      if (csvAutoCache && !csvRefresh && new File(path).exists())
        Some(ImageCsvImporter.imagesFromCsv(path))
      else None
  }

  private def currentYearImages(monumentDb: Some[MonumentDB])(yearContest: Contest): Future[ImageDB] = {
    val path = yearCsvPath(yearContest.year)
    if (csvAutoCache && !csvRefresh && new File(path).exists())
      syncCurrentYear(monumentDb, yearContest, path)
    else
      fetchImageDb(yearContest, monumentDb).map { db =>
        writeCsvCache(db)
        db
      }
  }

  /** Refresh the current-year CSV cache without a full refetch: keep cached
    * images still in the category, pull metadata only for newly uploaded ones.
    */
  private def syncCurrentYear(
      monumentDb: Some[MonumentDB],
      yearContest: Contest,
      path: String
  ): Future[ImageDB] = {
    val cached = ImageCsvImporter.imagesFromCsv(path)
    val cachedIds = cached.flatMap(_.pageId).toSet
    val query = imageQuery.getOrElse(liveImageQuery)
    for {
      liveIdsIt <- query.imageIdsFromCategory(yearContest)
      liveIds = liveIdsIt.toSet
      newIds = liveIds -- cachedIds
      newImages <-
        if (newIds.isEmpty) Future.successful(Iterable.empty[Image])
        else query.imagesWithTemplateByIds(yearContest, newIds)
    } yield {
      val kept = cached.filter(_.pageId.exists(liveIds.contains))
      val db = new ImageDB(yearContest, (kept ++ newImages).toSeq, monumentDb, config.minMpx)
      writeCsvCache(db)
      db
    }
  }

  private def fetchImageDb(
      yearContest: Contest,
      monumentDb: Some[MonumentDB]
  ): Future[ImageDB] =
    ImageDB.create(
      yearContest,
      imageQuery.getOrElse(getImageQuery(Some(yearContest.year))),
      monumentDb,
      config.minMpx
    )

  private def imagesFromCsvOpt(year: Int): Option[Seq[Image]] =
    config.imagesFromCsv.map { dir =>
      val path = ImageCsvExporter.filename(contest.campaign, year, isCurrent = false, dir)
      if (!new File(path).exists()) {
        throw new FileNotFoundException(
          s"--images-from-csv was set but $path is missing. " +
            s"Run --export-images-csv for campaign=${contest.campaign} year=$year first."
        )
      }
      ImageCsvImporter.imagesFromCsv(path)
    }

  private def imagesByTemplate(
      monumentDb: Some[MonumentDB],
      dbsByYear: Seq[ImageDB],
      totalPageIds: Iterable[Long]
  ): Future[ImageDB] = {
    val idsByYear = dbsByYear.flatMap(_.images.flatMap(_.pageId)).toSet
    val missingPageIds = totalPageIds.toSet -- idsByYear
    for {
      commons <- totalImageQuery.imagesWithTemplateByIds(contest, missingPageIds)
      wiki <- imageQueryWiki.map(_.imagesWithTemplate(contest)).getOrElse(Future.successful(Nil))
    } yield new ImageDB(contest, dbsByYear.flatMap(_.images) ++ commons ++ wiki, monumentDb)
  }

  private def imageIdsByTemplate(): Future[Iterable[Long]] =
    totalImageQuery.imageIdsWithTemplate(contest)

  def init(total: Boolean): Unit = {
    gatherData(total = total)
      .map { stat =>
        new ReporterRegistry(stat, config).output()
      }
      .failed
      .map(println)
  }

  def articleStatistics(monumentDb: MonumentDB): Unit = {
    println(Stats.withArticles(monumentDb).asWiki("Article Statistics").asWiki)
  }

  def toMassMessage(users: Iterable[String]): Iterable[String] = {
    users.map(name => s"{{#target:User talk:$name}}")
  }

  def message(bot: MwBot, user: String, msg: String => String): Unit = {
    bot.page("User_talk:" + user).edit(msg(user), section = Some("new"))
  }
}

object Statistics {

  def defaultCsvFilename(campaign: String): String = {
    val now = java.time.LocalDateTime.now()
    val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")
    s"$campaign-${now.format(fmt)}.csv"
  }

  def runExport(
      contest: Contest,
      cfg: StatConfig,
      monumentQuery: MonumentQuery
  ): Unit = {
    import scala.concurrent.Await
    import scala.concurrent.duration._
    val path = cfg.exportCsv.filter(_.nonEmpty).getOrElse(defaultCsvFilename(cfg.campaign))
    val maps = Await.result(monumentQuery.byMonumentTemplateMapsAsync(), 2.minutes)
    val mapping = org.scalawiki.wlx.UaUkJsonMapping.load("monuments_config/ua_uk.json")
    org.scalawiki.wlx.MonumentCsvExporter.export(maps, mapping, path)
  }

  def getContest(cfg: StatConfig): Contest = {
    val contest = Contest.byCampaign(cfg.campaign).getOrElse {
      throw new IllegalArgumentException(s"Unknown campaign: ${cfg.campaign}")
    }

    contest.copy(
      year = cfg.years.last,
      rateConfig = cfg.rateConfig
    )
  }

  def main(args: Array[String]): Unit = {
    val cfg = StatParams.parse(args)
    val contest = getContest(cfg)

    if (cfg.exportCsv.isDefined) {
      val monumentQuery = MonumentQuery.create(contest)
      runExport(contest, cfg, monumentQuery)
    }

    // Run the full statistics pipeline when either:
    // - no monument CSV export was requested (normal run), or
    // - image CSV export was requested (needs stats pipeline to populate dbsByYear)
    if (cfg.exportCsv.isEmpty || cfg.exportImagesCsv.isDefined) {
      val cacheName = s"${cfg.campaign}-${contest.year}"
      val imageQueryWiki = ImageQuery.create(
        new CachedBot(Site.ukWiki, cacheName + "-wiki", true, entries = 100)
      )

      val stat = new Statistics(
        contest,
        startYear = Some(cfg.years.head),
        monumentQuery = MonumentQuery.create(contest, reportDifferentRegionIds = true),
        config = Some(cfg),
        imageQuery = None,
        imageQueryWiki = Some(imageQueryWiki)
      )

      // rating fill needs the all-time image DB to know which monuments already
      // have photos, even when a single year is requested
      stat.init(total = cfg.years.size > 1 || cfg.fillListsRating)
    }
  }
}
