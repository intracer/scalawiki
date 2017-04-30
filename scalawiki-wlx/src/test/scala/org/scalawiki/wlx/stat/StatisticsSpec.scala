package org.scalawiki.wlx.stat

import org.joda.time.DateTime
import org.scalawiki.MwBot
import org.scalawiki.dto.Image
import org.scalawiki.wlx.{ImageDB, MonumentDB}
import org.scalawiki.wlx.dto._
import org.scalawiki.wlx.dto.lists.ListConfig._
import org.scalawiki.wlx.query.{ImageQuery, MonumentQuery}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import spray.util.pimpFuture

import scala.concurrent.Future

class StatisticsSpec(implicit ee: ExecutionEnv) extends Specification with Mockito with FutureMatchers {

  val contest = Contest.WLEUkraine(2016)

  def mockedStat(monuments: Seq[Monument], images: Seq[Image]): Statistics = {
    val bot = mock[MwBot]
    val monumentQuery = mock[MonumentQuery]
    val imageQuery = mock[ImageQuery]

    imageQuery.imagesFromCategoryAsync(contest.imagesCategory, contest) returns Future.successful(images)
    monumentQuery.byMonumentTemplate(date = None) returns monuments

    val cfg = StatConfig(campaign = contest.campaign)

    new Statistics(contest, None, monumentQuery, imageQuery, bot, cfg)
  }

  "statistics" should {

    "parse campaign" in {
      val year = DateTime.now.year().get()
      val cfg = Statistics.parse(Seq("-campaign", "wlm-ua"))
      cfg === StatConfig("wlm-ua", Seq(year), Nil)
    }

    "parse campaign with years" in {
      val cfg = Statistics.parse(Seq("-campaign", "wle-ua", "-year", "2015,2016"))
      cfg === StatConfig("wle-ua", Seq(2015, 2016), Nil)
    }

    "years sorted" in {
      val cfg = Statistics.parse(Seq("-campaign", "wle-ua", "-year", "2016,2014,2015,2012"))
      cfg === StatConfig("wle-ua", Seq(2012, 2014, 2015, 2016), Nil)
    }

    "parse campaign with regigons" in {
      val cfg = Statistics.parse(Seq("-campaign", "wle-ua", "-year", "2012",  "-region", "01,02"))
      cfg === StatConfig("wle-ua", Seq(2012), Seq("01", "02"))
    }

    "give current year stat" in {
      val monuments = Seq.empty[Monument]
      val images = Seq.empty[Image]

      val stat = mockedStat(monuments, images)
      val data = stat.gatherData().await

      data.contest === contest
      data.monumentDb.map(_.monuments) === Some(monuments)
      data.currentYearImageDb.images === images
      data.dbsByYear === Seq(data.currentYearImageDb)
      data.totalImageDb.isEmpty === true
    }
  }

  "give some stat" in {
    val images = Seq(Image("image1.jpg", author = Some("user"), monumentId = Some("123")))
    val monuments = Seq(new Monument(id = "123", name = "123 monument"))

    val stat = mockedStat(monuments, images)
    val data = stat.gatherData(total = false).await

    data.contest === contest
    data.monumentDb.map(_.monuments) === Some(monuments)
    data.currentYearImageDb.images === images
    data.dbsByYear === Seq(data.currentYearImageDb)
    data.totalImageDb.isEmpty === true
  }

  "handle image query error" in {

    val monuments = Seq.empty[Monument]

    val bot = mock[MwBot]
    val monumentQuery = mock[MonumentQuery]
    val imageQuery = mock[ImageQuery]

    imageQuery.imagesFromCategoryAsync(contest.imagesCategory, contest) returns Future.failed(new RuntimeException("Error 123"))
    monumentQuery.byMonumentTemplate(date = None) returns monuments

    val stat = new Statistics(contest, None, monumentQuery, imageQuery, bot)

    stat.gatherData() must throwA[RuntimeException].await
  }

  def monument(id: String, name: String) =
    new Monument(id = id, name = name, listConfig = Some(WlmUa))

  def monuments(n: Int, regionId: String, namePrefix: String, startId: Int = 1): Seq[Monument] =
    (startId until startId + n).map(i => monument(s"$regionId-xxx-000$i", namePrefix + i))

  "getOldImagesMonumentDb" should {
    val bot = mock[MwBot]
    val monumentQuery = mock[MonumentQuery]
    val imageQuery = mock[ImageQuery]
    val stat = new Statistics(contest, None, monumentQuery, imageQuery, bot)

    "be empty" in {
      stat.getOldImagesMonumentDb(None, None, None, new ImageDB(contest, Seq.empty)) === None

      val mDb = new MonumentDB(contest, Seq.empty)
      stat.getOldImagesMonumentDb(Some(mDb), None, None, new ImageDB(contest, Seq.empty)) === None
    }

    "have images from old monument db" in {

      val images1 = Seq(Image("File:Img11y1f1.jpg", monumentId = Some("01-xxx-0001"), author = Some("FromCrimea")))

      val images2 = Seq(Image("File:Img11y2f1.jpg", monumentId = Some("01-xxx-0002"), author = Some("FromCrimeaOld")))

      val withoutPhotos = monuments(3, "01", "Crimea")
      val withPhotos = withoutPhotos.head.copy(photo = Some(images1.head.title)) +: withoutPhotos.tail
      val mDb = new MonumentDB(contest, withPhotos)

      val oldMdb = stat.getOldImagesMonumentDb(Some(mDb), Some(mDb), None, new ImageDB(contest, images2))

      oldMdb.map(_.monuments) === Some(Seq(withPhotos.head))
    }

  }
}


