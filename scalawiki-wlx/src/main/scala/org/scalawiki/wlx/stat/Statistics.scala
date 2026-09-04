package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.cache.CachedBot
import org.scalawiki.dto.{Image, Site}
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.query.ImageQuery.PageRevInfo
import org.scalawiki.wlx.query.{ImageQuery, MonumentQuery}
import org.scalawiki.wlx.stat.reports.ReporterRegistry
import org.scalawiki.wlx.{ImageCsvExporter, ImageCsvImporter, ImageDB, MonumentDB}

import java.io.{File, FileNotFoundException}
import java.time.ZonedDateTime

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
    val totalCsvPath =
      if (csvRefresh) None else totalCsvReadPath.filter(new File(_).exists())
    val totalPageRevsFuture =
      if (total && totalCsvPath.isEmpty) imageRevsByTemplate() else Future.successful(Nil)
    for {
      byYear <- Future.sequence(byYearFutures)
      currentYearImages = byYear.last
      totalPageRevs <- totalPageRevsFuture
      totalImages <-
        if (!total) Future.successful(currentYearImages)
        else
          totalCsvPath match {
            case Some(path) if csvResync =>
              resyncTotalCsv(monumentDb, path)
            case Some(path) =>
              Future.successful(
                new ImageDB(contest, ImageCsvImporter.imagesFromCsv(path), monumentDb, config.minMpx)
              )
            case None =>
              imagesByTemplate(monumentDb, byYear, totalPageRevs).map { db =>
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
  // - the current contest year is always incrementally synced against the wiki:
  //   a cheap id + latest-revision sweep of the category tells us which files are
  //   new (fetch metadata), which changed since caching (revid differs -> refetch)
  //   and which are gone (dropped). Its CSV is written to the same
  //   `<campaign>-<year>-images.csv` path that next year's run reads as the frozen
  //   past-year copy -- so the last mid-contest sync of year N becomes the
  //   permanent record of year N.
  // - past contest years and the all-images CSV are frozen (read verbatim) unless
  //   `--csv-cache-resync` is given, which runs the same new/changed/deleted sweep
  //   against them. Rows written before the `last_revid` column existed fall back
  //   to a per-row timestamp: a change counts only if the live revision post-dates
  //   the contest's end for that year (`contestEndInstant`).
  // - delete a CSV to force a full refetch (removing only the `.cache` does
  //   nothing, the CSV short-circuits before the ChronicleMap is consulted).
  // - `--csv-cache-refresh` ignores existing CSVs and overwrites them.

  private val csvStrictDir: Option[String] = config.imagesFromCsv
  private val csvAutoCache: Boolean = config.csvCache && csvStrictDir.isEmpty
  private val csvDir: String = config.effectiveCsvCacheDir
  private val csvRefresh: Boolean = config.csvCacheRefresh && csvAutoCache
  private val csvResync: Boolean = config.csvCacheResync && csvAutoCache && !csvRefresh

  private lazy val liveImageQuery: ImageQuery = ImageQuery.create

  private val knownContestYears: Seq[Int] = contests.map(_.year)

  /** End of the upload window for a contest year: `contest.endDate` ("dd-MM")
    * applied to `year` when configured, otherwise the end of that calendar year.
    * Used as the "cache was accurate until" instant for CSV rows written before
    * the `last_revid` column existed.
    */
  private def contestEndInstant(year: Int): ZonedDateTime = {
    val fromConfig = contest.endDate match {
      case s if s.matches("""\d{1,2}-\d{1,2}""") =>
        val Array(d, m) = s.split("-")
        scala.util
          .Try(ZonedDateTime.parse(f"$year%04d-${m.toInt}%02d-${d.toInt}%02dT23:59:59Z"))
          .toOption
      case _ => None
    }
    fromConfig.getOrElse(ZonedDateTime.parse(f"$year%04d-12-31T23:59:59Z"))
  }

  /** The contest year an image counts against: the latest known contest year not
    * after its upload year (falls back to the upload year itself). */
  private def contestYearFor(uploadYear: Int): Int =
    knownContestYears.filter(_ <= uploadYear).lastOption.getOrElse(uploadYear)

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
    val path = yearCsvPath(year)
    csvStrictDir match {
      case Some(_) =>
        // strict, user-managed CSVs: read verbatim (throws if missing)
        Future.successful(
          new ImageDB(yearContest, imagesFromCsvOpt(year).getOrElse(Nil), monumentDb, config.minMpx)
        )
      case None if csvAutoCache && !csvRefresh && new File(path).exists() =>
        val cached = ImageCsvImporter.imagesFromCsv(path)
        if (!csvResync)
          Future.successful(new ImageDB(yearContest, cached, monumentDb, config.minMpx))
        else {
          val query = imageQuery.getOrElse(liveImageQuery)
          query.imageIdsFromCategory(yearContest).flatMap { liveRevs =>
            syncImageDb(
              yearContest,
              monumentDb,
              cached,
              writeCsvCache,
              liveRevs,
              _ => contestEndInstant(year),
              ids => query.imagesWithTemplateByIds(yearContest, ids)
            )
          }
        }
      case None =>
        fetchImageDb(yearContest, monumentDb).map { db =>
          writeCsvCache(db)
          db
        }
    }
  }

  private def currentYearImages(monumentDb: Some[MonumentDB])(yearContest: Contest): Future[ImageDB] = {
    val path = yearCsvPath(yearContest.year)
    if (csvAutoCache && !csvRefresh && new File(path).exists()) {
      val cached = ImageCsvImporter.imagesFromCsv(path)
      val query = imageQuery.getOrElse(liveImageQuery)
      query.imageIdsFromCategory(yearContest).flatMap { liveRevs =>
        syncImageDb(
          yearContest,
          monumentDb,
          cached,
          writeCsvCache,
          liveRevs,
          _ => contestEndInstant(yearContest.year),
          ids => query.imagesWithTemplateByIds(yearContest, ids)
        )
      }
    } else
      fetchImageDb(yearContest, monumentDb).map { db =>
        writeCsvCache(db)
        db
      }
  }

  /** Incrementally reconcile a cached image set against a fresh id + latest-revision
    * sweep of the wiki:
    *   - ids in the sweep but not the cache  -> fetched (new uploads)
    *   - ids in both whose revision changed  -> refetched (page edited / reuploaded)
    *   - ids in the cache but not the sweep  -> dropped (deleted / de-categorised)
    *   - unchanged rows are kept as-is (revid/timestamp backfilled from the sweep)
    *
    * "changed" is `revId` mismatch when the cached row has one; otherwise (rows
    * written before the column existed) the live revision timestamp being after
    * `fallbackTs(row)`.
    *
    * `extraImages` are appended unconditionally (e.g. uk.wikipedia-hosted images
    * for the all-images CSV, which live in a different page-id space).
    */
  private def syncImageDb(
      yearContest: Contest,
      monumentDb: Option[MonumentDB],
      cached: Seq[Image],
      writeCache: ImageDB => Unit,
      liveRevs: Seq[PageRevInfo],
      fallbackTs: Image => ZonedDateTime,
      fetch: Set[Long] => Future[Iterable[Image]],
      extraImages: Iterable[Image] = Nil
  ): Future[ImageDB] = {
    val liveById = liveRevs.iterator.map(r => r.pageId -> r).toMap
    val cachedById = cached.iterator.flatMap(i => i.pageId.map(_ -> i)).toMap

    val newIds = liveById.keySet -- cachedById.keySet
    val changedIds = (liveById.keySet intersect cachedById.keySet).filter { id =>
      val live = liveById(id)
      val row = cachedById(id)
      row.revId match {
        case Some(rid) => rid != live.revId
        case None      => live.timestamp.isAfter(row.revTs.getOrElse(fallbackTs(row)))
      }
    }
    val refetch = newIds ++ changedIds

    val kept = cached.collect {
      case i if i.pageId.exists(id => liveById.contains(id) && !changedIds.contains(id)) =>
        val live = liveById(i.pageId.get)
        i.copy(revId = Some(live.revId), revTs = Some(live.timestamp))
    }

    val fetchedFuture =
      if (refetch.isEmpty) Future.successful(Iterable.empty[Image]) else fetch(refetch)
    fetchedFuture.map { fetched =>
      val db = new ImageDB(
        yearContest,
        (kept ++ fetched ++ extraImages).toSeq,
        monumentDb,
        config.minMpx
      )
      writeCache(db)
      db
    }
  }

  /** Resync the all-images CSV: revid sweep of the Commons contest template,
    * unioned with a fresh fetch of the uk.wikipedia-hosted images (small set,
    * different page-id space, so always refetched rather than diffed). */
  private def resyncTotalCsv(
      monumentDb: Option[MonumentDB],
      path: String
  ): Future[ImageDB] = {
    val cached = ImageCsvImporter.imagesFromCsv(path)
    for {
      commonsRevs <- totalImageQuery.imageIdsWithTemplate(contest)
      wiki <- imageQueryWiki.map(_.imagesWithTemplate(contest)).getOrElse(Future.successful(Nil))
      wikiIds = wiki.flatMap(_.pageId).toSet
      cachedCommons = cached.filterNot(_.pageId.exists(wikiIds.contains))
      db <- syncImageDb(
        contest,
        monumentDb,
        cachedCommons,
        writeTotalCsvCache,
        commonsRevs,
        row => contestEndInstant(contestYearFor(row.date.map(_.getYear).getOrElse(currentYear - 1))),
        ids => totalImageQuery.imagesWithTemplateByIds(contest, ids),
        extraImages = wiki
      )
    } yield db
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
      totalPageRevs: Seq[PageRevInfo]
  ): Future[ImageDB] = {
    val idsByYear = dbsByYear.flatMap(_.images.flatMap(_.pageId)).toSet
    val missingPageIds = totalPageRevs.map(_.pageId).toSet -- idsByYear
    for {
      commons <- totalImageQuery.imagesWithTemplateByIds(contest, missingPageIds)
      wiki <- imageQueryWiki.map(_.imagesWithTemplate(contest)).getOrElse(Future.successful(Nil))
    } yield new ImageDB(contest, dbsByYear.flatMap(_.images) ++ commons ++ wiki, monumentDb)
  }

  private def imageRevsByTemplate(): Future[Seq[PageRevInfo]] =
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
