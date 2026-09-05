package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.cache.CachedBot
import org.scalawiki.dto.{Image, Site}
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.query.ImageQuery.PageRevInfo
import org.scalawiki.wlx.query.{ImageQuery, MonumentQuery}
import org.scalawiki.wlx.stat.progress.Progress
import org.scalawiki.wlx.stat.reports.ReporterRegistry
import org.scalawiki.util.WriteWatcher
import org.scalawiki.wlx.{ImageCsvExporter, ImageCsvImporter, ImageDB, MonumentDB}

import org.slf4j.LoggerFactory

import java.io.{File, FileNotFoundException}
import java.nio.file.{Files, Paths}
import java.time.{ZoneOffset, ZonedDateTime}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try
import scala.util.control.NonFatal

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

  private val logger = LoggerFactory.getLogger(classOf[Statistics])

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
    val monumentDb = Some(
      Progress.phase("Fetching monument lists")(
        MonumentDB.getMonumentDb(contest, monumentQuery)
      )
    )

    val byYearLabel =
      if (contests.sizeIs > 1) s"Fetching images ${contests.head.year}-${contests.last.year}"
      else s"Fetching images ${contests.head.year}"
    val byYearF =
      Progress.barF(byYearLabel, contests.size.toLong) { task =>
        Future.sequence(contests.map { yearContest =>
          contestImages(monumentDb)(yearContest).map { db =>
            task.step()
            db
          }
        })
      }

    val totalCsvPath =
      if (csvRefresh) None else totalCsvReadPath.filter(new File(_).exists())
    val totalPageRevsFuture =
      if (total && totalCsvPath.isEmpty) imageRevsByTemplate() else Future.successful(Nil)
    for {
      byYear <- byYearF
      currentYearImages = byYear.last
      totalPageRevs <- totalPageRevsFuture
      totalImages <-
        if (!total) Future.successful(currentYearImages)
        else
          totalCsvPath match {
            case Some(path) if csvResync =>
              resyncTotalCsv(monumentDb, byYear, path)
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
  // A second-tier cache above the `http-cache/` request cache: once an
  // `ImageDB` has been built it is serialized to `<csvDir>/<campaign>-<year>-images.csv`
  // (and `<campaign>-all-images.csv` for the all-time DB). Later runs read those
  // CSVs directly and skip the sequential JSON parse of the raw API responses.
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
  //   against them. For rows written before the `last_revid` column existed we
  //   have no revid to compare, so a change is assumed only when the live
  //   revision post-dates the moment the CSV was last written (its file mtime).
  // - deletions are only trusted when the sweep looks complete: the number of
  //   ids it returned is checked against `categoryinfo.files` (and, failing that,
  //   against the cached row count). A short sweep (truncated pagination, a
  //   transient API hiccup) keeps every cached row rather than wiping the CSV.
  // - delete a CSV to force a full refetch (clearing only `http-cache/` does
  //   nothing, the CSV short-circuits before the request cache is consulted).
  // - `--csv-cache-refresh` ignores existing CSVs and overwrites them.

  private val csvStrictDir: Option[String] = config.imagesFromCsv
  private val csvAutoCache: Boolean = config.csvCache && csvStrictDir.isEmpty
  private val csvDir: String = config.effectiveCsvCacheDir
  private val csvRefresh: Boolean = config.csvCacheRefresh && csvAutoCache
  private val csvResync: Boolean = config.csvCacheResync && csvAutoCache && !csvRefresh

  private lazy val liveImageQuery: ImageQuery = ImageQuery.create

  /** When the CSV at `path` was last written — the instant the cache was known
    * accurate. Used as the "changed since" cut-off for rows that predate the
    * `last_revid` column (no revid to diff). Falls back to "now" (nothing looks
    * changed) if the mtime can't be read, keeping the first resync cheap. */
  private def cacheWrittenAt(path: String): ZonedDateTime =
    Try(Files.getLastModifiedTime(Paths.get(path)).toInstant.atZone(ZoneOffset.UTC))
      .getOrElse(ZonedDateTime.now(ZoneOffset.UTC))

  /** Whether an id sweep of `swept` entries can be trusted to be exhaustive
    * enough to act on deletions. `categoryinfo.files` is the reference when
    * available (it lags reality by a job-queue cycle, hence the 10% slack);
    * without it, only a sweep that still covers most of the cached rows is
    * trusted. An empty sweep against a non-empty cache never is. */
  private def sweepLooksComplete(swept: Int, cachedCount: Int, expectedFiles: Option[Long]): Boolean =
    if (swept == 0 && cachedCount > 0) false
    else
      expectedFiles match {
        case Some(expected) => swept >= expected * 0.9
        case None           => swept >= cachedCount * 0.5
      }

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
        else
          syncYearFromCategory(yearContest, monumentDb, cached, path)
      case None =>
        fetchImageDb(yearContest, monumentDb).map { db =>
          writeCsvCache(db)
          db
        }
    }
  }

  private def currentYearImages(monumentDb: Some[MonumentDB])(yearContest: Contest): Future[ImageDB] = {
    val path = yearCsvPath(yearContest.year)
    if (csvAutoCache && !csvRefresh && new File(path).exists())
      syncYearFromCategory(yearContest, monumentDb, ImageCsvImporter.imagesFromCsv(path), path)
    else
      fetchImageDb(yearContest, monumentDb).map { db =>
        writeCsvCache(db)
        db
      }
  }

  /** Reconcile a per-year CSV cache against a fresh category id + revision sweep.
    * Shared by the always-on current-year sync and the `--csv-cache-resync`
    * past-year sync. */
  private def syncYearFromCategory(
      yearContest: Contest,
      monumentDb: Some[MonumentDB],
      cached: Seq[Image],
      path: String
  ): Future[ImageDB] = {
    val query = imageQuery.getOrElse(liveImageQuery)
    // "changed since" cut-off for pre-last_revid rows: once a past year's upload
    // window has closed nothing legitimate changes after it, so it is the exact
    // instant the cache became authoritative. Mid-contest (window end still in
    // the future) fall back to when the CSV was last written.
    val now = ZonedDateTime.now(ZoneOffset.UTC)
    val cutoff = yearContest
      .dates()
      .flatMap(_.uploadEndInstant)
      .filter(_.isBefore(now))
      .getOrElse(cacheWrittenAt(path))
    for {
      liveRevs <- query.imageIdsFromCategory(yearContest)
      expectedFiles <- query.categoryFileCount(yearContest)
      db <- syncImageDb(
        yearContest,
        monumentDb,
        cached,
        writeCsvCache,
        liveRevs,
        _ => cutoff,
        ids => query.imagesWithTemplateByIds(yearContest, ids),
        sweepComplete =
          sweepLooksComplete(liveRevs.size, cached.count(_.pageId.isDefined), expectedFiles)
      )
    } yield db
  }

  /** Incrementally reconcile a cached image set against a fresh id + latest-revision
    * sweep of the wiki:
    *   - ids in the sweep but not the cache  -> fetched (new uploads)
    *   - ids in both whose revision changed  -> refetched (page edited / reuploaded)
    *   - ids in the cache but not the sweep  -> dropped, *only* when `sweepComplete`
    *   - everything else kept as-is (revid/timestamp backfilled from the sweep)
    *
    * "changed" is a `revId` mismatch when both the cached row and the sweep entry
    * expose one; for rows written before the column existed (no cached revid) it
    * is the live revision timestamp being after `fallbackTs(row)`. A sweep entry
    * with no revid (revision-deleted current revision) is treated as unchanged.
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
      extraImages: Iterable[Image] = Nil,
      sweepComplete: Boolean = true
  ): Future[ImageDB] = {
    val liveById = liveRevs.iterator.map(r => r.pageId -> r).toMap
    val cachedById = cached.iterator.flatMap(i => i.pageId.map(_ -> i)).toMap

    val newIds = liveById.keySet -- cachedById.keySet
    val changedIds = (liveById.keySet intersect cachedById.keySet).filter { id =>
      val live = liveById(id)
      val row = cachedById(id)
      (row.revId, live.revId) match {
        case (Some(cachedRev), Some(liveRev)) => cachedRev != liveRev
        case (Some(_), None)                  => false // revdel'd sweep entry: can't tell, keep
        case (None, _) =>
          live.timestamp.exists(_.isAfter(row.revTs.getOrElse(fallbackTs(row))))
      }
    }
    val refetch = newIds ++ changedIds

    val goneIds =
      if (sweepComplete) cachedById.keySet -- liveById.keySet
      else {
        val missing = cachedById.keySet -- liveById.keySet
        if (missing.nonEmpty)
          logger.warn(
            s"[csv-cache] ${yearContest.year}: sweep returned ${liveById.size} ids for " +
              s"${cachedById.size} cached rows — treating it as incomplete, keeping " +
              s"${missing.size} unmatched row(s) instead of deleting them"
          )
        Set.empty[Long]
      }

    def backfill(i: Image): Image =
      i.pageId.flatMap(liveById.get) match {
        case Some(live) =>
          i.copy(revId = live.revId.orElse(i.revId), revTs = live.timestamp.orElse(i.revTs))
        case None => i
      }

    val kept = cached.collect {
      case i
          if i.pageId.exists(id => !refetch.contains(id) && !goneIds.contains(id)) ||
            i.pageId.isEmpty =>
        backfill(i)
    }

    val fetchedFuture =
      if (refetch.isEmpty) Future.successful(Iterable.empty[Image]) else fetch(refetch)
    fetchedFuture.map { fetched =>
      val fetchedIds = fetched.flatMap(_.pageId).toSet
      // A partial refetch (transient error resolving some ids) must not silently
      // drop a row we still know about: fall back to the stale cached copy.
      val missedRefetch = refetch -- fetchedIds
      val staleKept = cached.filter(_.pageId.exists(id => missedRefetch.contains(id) && !newIds.contains(id)))
      if (missedRefetch.nonEmpty)
        logger.warn(
          s"[csv-cache] ${yearContest.year}: refetch returned ${fetchedIds.size}/${refetch.size} " +
            s"images; keeping ${staleKept.size} stale row(s), ${(missedRefetch -- staleKept.flatMap(_.pageId).toSet).size} new id(s) lost this run"
        )

      val db = new ImageDB(
        yearContest,
        dedupByPageId(kept ++ fetched ++ staleKept ++ extraImages),
        monumentDb,
        config.minMpx
      )
      writeCache(db)
      db
    }
  }

  /** Keep the first image seen for each page id (rows with no page id pass
    * through). Order of preference is the caller's list order. */
  private def dedupByPageId(images: Iterable[Image]): Seq[Image] = {
    val seen = scala.collection.mutable.Set.empty[Long]
    images.iterator.filter { i =>
      i.pageId match {
        case Some(id) => seen.add(id)
        case None     => true
      }
    }.toVector
  }

  /** A cached row is hosted on a project wiki (not Commons) when its stored
    * `page_url` points somewhere other than commons.wikimedia.org. Those rows are
    * outside the Commons template sweep's page-id space, so the sweep must never
    * be allowed to treat them as deleted. */
  private def isProjectWikiHosted(image: Image): Boolean =
    image.pageUrl.exists(url => !url.contains("commons.wikimedia.org"))

  /** Resync the all-images CSV: a live revid sweep of the Commons contest
    * template diffed against the Commons-hosted cached rows, plus a fresh fetch
    * of the uk.wikipedia-hosted images (small set, different page-id space, so
    * always refetched rather than diffed), plus the per-year images (kept in sync
    * with the full-rebuild path, which unions them too). */
  private def resyncTotalCsv(
      monumentDb: Option[MonumentDB],
      dbsByYear: Seq[ImageDB],
      path: String
  ): Future[ImageDB] = {
    val cached = ImageCsvImporter.imagesFromCsv(path)
    val (cachedWiki, cachedCommons) = cached.partition(isProjectWikiHosted)
    val writtenAt = cacheWrittenAt(path)
    val query = imageQuery.getOrElse(liveImageQuery)
    val perYearImages = dbsByYear.flatMap(_.images)
    for {
      commonsRevs <- query.imageIdsWithTemplate(contest)
      freshWiki <- imageQueryWiki.map(_.imagesWithTemplate(contest)).getOrElse(Future.successful(Nil))
      // an empty uk.wiki refetch when one was configured means the fetch failed;
      // fall back to the cached wiki rows and, since some of those may sit in
      // cachedCommons (rows cached without a page_url can't be told apart), also
      // stop trusting the sweep for deletions this run
      wikiFetchTrustworthy = freshWiki.nonEmpty || imageQueryWiki.isEmpty
      wiki =
        if (wikiFetchTrustworthy) freshWiki
        else {
          logger.warn(
            "[csv-cache] all-images: uk.wiki refetch returned nothing — keeping all cached " +
              "rows and skipping Commons deletion detection this run"
          )
          cachedWiki
        }
      db <- syncImageDb(
        contest,
        monumentDb,
        cachedCommons,
        writeTotalCsvCache,
        commonsRevs,
        _ => writtenAt,
        ids => query.imagesWithTemplateByIds(contest, ids),
        extraImages = wiki ++ perYearImages,
        sweepComplete = wikiFetchTrustworthy &&
          sweepLooksComplete(commonsRevs.size, cachedCommons.count(_.pageId.isDefined), None)
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
    Progress.phaseF(s"Fetching all-time images (${missingPageIds.size} files)") {
      for {
        commons <- totalImageQuery.imagesWithTemplateByIds(contest, missingPageIds)
        wiki <- imageQueryWiki.map(_.imagesWithTemplate(contest)).getOrElse(Future.successful(Nil))
      } yield new ImageDB(contest, dbsByYear.flatMap(_.images) ++ commons ++ wiki, monumentDb)
    }
  }

  private def imageRevsByTemplate(): Future[Seq[PageRevInfo]] =
    totalImageQuery.imageIdsWithTemplate(contest)

  /** Fetch contest data, run every configured report, and block until every
    * wiki write has settled.
    *
    * Unlike the old fire-and-forget version this waits for the whole pipeline,
    * isolates each report step (one failing step no longer aborts the rest),
    * and reports what did not publish.
    *
    * @return the number of failures (report steps that threw + wiki writes that
    *         errored). 0 means a clean run.
    */
  def run(total: Boolean): Int = {
    Progress.configure(config.progress)
    try {
      val stat = Await.result(gatherData(total = total), Duration.Inf)

      val stepErrors =
        Progress.bar("Generating reports", 0L) { task =>
          new ReporterRegistry(stat, config, Some(task)).output()
        }
      // Publishing is the long pole on a cached run: dozens of throttled edits
      // draining a few at a time. Drive a bar off WriteWatcher's counters.
      val writeFailures =
        Progress.bar("Publishing edits", WriteWatcher.submittedCount) { task =>
          WriteWatcher.awaitQuiescence(onProgress = (done, submitted) => {
            task.total(submitted)
            task.stepTo(done)
          })
        }

      if (stepErrors.nonEmpty || writeFailures.nonEmpty) {
        Progress.note("\n=== Publish summary: INCOMPLETE ===")
        if (stepErrors.nonEmpty) {
          Progress.note(s"report steps that failed: ${stepErrors.size}")
          stepErrors.foreach { case (name, e) => Progress.note(s"  - $name: $e") }
        }
        if (writeFailures.nonEmpty) {
          Progress.note(
            s"wiki writes that errored: ${writeFailures.size} " +
              "(edit conflicts the list updater retries are excluded)"
          )
          writeFailures.foreach { case (desc, e) => Progress.note(s"  - $desc: $e") }
        }
      } else {
        Progress.note(
          s"\n=== Publish summary: OK (${WriteWatcher.completedCount} wiki writes) ==="
        )
      }

      stepErrors.size + writeFailures.size
    } finally Progress.close()
  }

  def init(total: Boolean): Unit = {
    run(total)
    ()
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
    // logback reads these system properties when it first initialises (about to
    // happen, on the first LoggerFactory call below). `--verbose` lifts the
    // console appender from WARN to INFO (the per-request detail that always goes
    // to logs/scalawiki.log now shows on screen too) and the root logger from
    // INFO to DEBUG (so DEBUG detail reaches the file). Checked directly (not via
    // StatParams) to run before any logger is created; the flag is also declared
    // in StatParams for --help.
    if (args.contains("--verbose") || args.contains("-v")) {
      System.setProperty("sw.console.level", "INFO")
      System.setProperty("sw.root.level", "DEBUG")
    }

    // Track every wiki edit/upload so failures are logged and `main` can wait
    // for them all before shutting the process down.
    WriteWatcher.enable(MwBot.system.log)

    var exitCode = 0
    try {
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
          new CachedBot(Site.ukWiki, cacheName + "-wiki", true)
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
        exitCode = stat.run(total = cfg.years.size > 1 || cfg.fillListsRating)
      }
    } catch {
      case NonFatal(e) =>
        println(s"Statistics run failed: $e")
        e.printStackTrace()
        exitCode = 1
    } finally {
      // Stop the Pekko ActorSystem so its non-daemon threads no longer keep the
      // JVM alive; without this the process hangs after all reports are done.
      try Await.result(MwBot.system.terminate(), 30.seconds)
      catch { case NonFatal(_) => }
    }

    System.exit(exitCode)
  }
}
