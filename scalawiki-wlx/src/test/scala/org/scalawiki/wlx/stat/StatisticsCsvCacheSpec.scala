package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.scalawiki.wlx.query.{ImageQuery, MonumentQuery}
import org.scalawiki.wlx.{ImageCsvExporter, ImageCsvImporter, ImageDB}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import spray.util.pimpFuture

import java.nio.file.{Files, Path, Paths}
import scala.concurrent.Future

class StatisticsCsvCacheSpec(implicit ee: ExecutionEnv)
    extends Specification
    with Mockito
    with FutureMatchers {

  private val contest = Contest.WLEUkraine(2016)
  private val prevContest = contest.copy(year = 2015)
  private val campaign = contest.campaign

  private val monuments = Seq(new Monument(id = "123", name = "m"))

  private def img(title: String, id: Long): Image =
    Image(title, pageId = Some(id), monumentIds = Seq("123"))

  private def cacheDir(): Path = Files.createTempDirectory("stats-csv-cache")

  private def yearCsv(dir: Path, year: Int) =
    Paths.get(ImageCsvExporter.filename(campaign, year, isCurrent = false, dir.toString))

  private def rowCount(p: Path): Int =
    ImageCsvImporter.imagesFromCsv(p.toString).size

  private def newImageQuery(): ImageQuery = {
    val q = mock[ImageQuery]
    // safe defaults so an unexpected sync path doesn't NPE
    q.imageIdsFromCategory(any[Contest]) returns Future.successful(Nil)
    q
  }

  private def stats(
      dir: Path,
      imageQuery: ImageQuery,
      startYear: Option[Int] = None,
      csvCache: Boolean = true,
      csvCacheRefresh: Boolean = false
  ): Statistics = {
    val monumentQuery = mock[MonumentQuery]
    monumentQuery.byMonumentTemplate(date = None) returns monuments

    val cfg = StatConfig(
      campaign = campaign,
      csvCacheDir = dir.toString,
      csvCache = csvCache,
      csvCacheRefresh = csvCacheRefresh
    )
    new Statistics(contest, startYear, monumentQuery, Some(imageQuery), None, mock[MwBot], cfg)
  }

  "past contest year" should {

    "be fetched then written to the CSV cache on the first run, read on the next" in {
      val dir = cacheDir()
      val pastImages = Seq(img("File:A.jpg", 1L))

      val q1 = newImageQuery()
      q1.imagesFromCategory(prevContest) returns Future.successful(pastImages)
      q1.imagesFromCategory(contest) returns Future.successful(Nil)

      stats(dir, q1, startYear = Some(2015)).gatherData(total = false).await

      Files.exists(yearCsv(dir, 2015)) must beTrue

      // second run: a fresh query mock that fails if asked for 2015
      val q2 = newImageQuery()
      q2.imagesFromCategory(prevContest) returns Future.failed(new RuntimeException("should not fetch"))
      q2.imagesFromCategory(contest) returns Future.successful(Nil)

      val data = stats(dir, q2, startYear = Some(2015)).gatherData(total = false).await

      data.imageDbByYear(2015).map(_.images.map(_.title).toSeq) must beSome(Seq("File:A.jpg"))
      there was no(q2).imagesFromCategory(prevContest)
    }

    "be refetched and overwritten when csvCacheRefresh is set" in {
      val dir = cacheDir()
      ImageCsvExporter.export(new ImageDB(prevContest, Seq(img("File:Stale.jpg", 9L)), None), campaign,
        isCurrent = false, dir.toString)

      val q = newImageQuery()
      q.imagesFromCategory(prevContest) returns Future.successful(Seq(img("File:Fresh.jpg", 1L)))
      q.imagesFromCategory(contest) returns Future.successful(Nil)

      val data = stats(dir, q, startYear = Some(2015), csvCacheRefresh = true)
        .gatherData(total = false).await

      data.imageDbByYear(2015).map(_.images.map(_.title).toSeq) must beSome(Seq("File:Fresh.jpg"))
      ImageCsvImporter.imagesFromCsv(yearCsv(dir, 2015).toString).map(_.title) must_== Seq("File:Fresh.jpg")
    }
  }

  "current contest year" should {

    "incrementally sync: keep cached images, pull only newly added ids" in {
      val dir = cacheDir()
      ImageCsvExporter.export(new ImageDB(contest, Seq(img("File:Old.jpg", 1L)), None), campaign,
        isCurrent = false, dir.toString)

      val q = newImageQuery()
      q.imageIdsFromCategory(contest) returns Future.successful(Seq(1L, 2L))
      q.imagesWithTemplateByIds(contest, Set(2L)) returns Future.successful(Seq(img("File:New.jpg", 2L)))

      val data = stats(dir, q).gatherData(total = false).await

      data.currentYearImageDb.images.map(_.title).toSet must_== Set("File:Old.jpg", "File:New.jpg")
      rowCount(yearCsv(dir, 2016)) must_== 2
      there was no(q).imagesFromCategory(contest)
    }

    "drop cached images no longer in the category" in {
      val dir = cacheDir()
      ImageCsvExporter.export(
        new ImageDB(contest, Seq(img("File:Keep.jpg", 1L), img("File:Gone.jpg", 2L)), None),
        campaign, isCurrent = false, dir.toString)

      val q = newImageQuery()
      q.imageIdsFromCategory(contest) returns Future.successful(Seq(1L))

      val data = stats(dir, q).gatherData(total = false).await

      data.currentYearImageDb.images.map(_.title).toSeq must_== Seq("File:Keep.jpg")
      there was no(q).imagesWithTemplateByIds(any[Contest], any[Set[Long]])
    }

    "be fully fetched and cached when no CSV exists yet" in {
      val dir = cacheDir()
      val q = newImageQuery()
      q.imagesFromCategory(contest) returns Future.successful(Seq(img("File:A.jpg", 1L)))

      val data = stats(dir, q).gatherData(total = false).await

      data.currentYearImageDb.images.map(_.title).toSeq must_== Seq("File:A.jpg")
      rowCount(yearCsv(dir, 2016)) must_== 1
    }
  }

  "csvCache = false" should {
    "always fetch and never touch the CSV cache" in {
      val dir = cacheDir()
      ImageCsvExporter.export(new ImageDB(contest, Seq(img("File:Cached.jpg", 1L)), None), campaign,
        isCurrent = false, dir.toString)

      val q = newImageQuery()
      q.imagesFromCategory(contest) returns Future.successful(Seq(img("File:Fetched.jpg", 7L)))

      val data = stats(dir, q, csvCache = false).gatherData(total = false).await

      data.currentYearImageDb.images.map(_.title).toSeq must_== Seq("File:Fetched.jpg")
      there was no(q).imageIdsFromCategory(contest)
      // cache file left untouched
      ImageCsvImporter.imagesFromCsv(yearCsv(dir, 2016).toString).map(_.title) must_== Seq("File:Cached.jpg")
    }
  }
}
