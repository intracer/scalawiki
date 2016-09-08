package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto._
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

    imageQuery.imagesFromCategoryAsync(contest.category, contest) returns Future.successful(images)
    monumentQuery.byMonumentTemplate(date = None) returns monuments

    new Statistics(contest, None, monumentQuery, imageQuery, bot)
  }

  "statistics" should {
    "give empty stat" in {
      val monuments = Seq.empty[Monument]
      val images = Seq.empty[Image]

      val stat = mockedStat(monuments, images)
      val data = stat.gatherData().await

      data.contest === contest
      data.monumentDb.map(_.monuments) === Some(monuments)
      data.currentYearImageDb.images === images
      data.dbsByYear === Seq.empty
      data.totalImageDb.isEmpty === true
    }
  }

  "give some stat" in {
    val images = Seq(Image("image1.jpg", author = Some("user"), monumentId = Some("123")))
    val monuments = Seq(new Monument(id = "123", name = "123 monument"))

    val stat = mockedStat(monuments, images)
    val data = stat.gatherData(total = false, byYear = false).await

    data.contest === contest
    data.monumentDb.map(_.monuments) === Some(monuments)
    data.currentYearImageDb.images === images
    data.dbsByYear === Seq.empty
    data.totalImageDb.isEmpty === true
  }

  "handle image query error" in {

    val monuments = Seq.empty[Monument]

    val bot = mock[MwBot]
    val monumentQuery = mock[MonumentQuery]
    val imageQuery = mock[ImageQuery]

    imageQuery.imagesFromCategoryAsync(contest.category, contest) returns Future.failed(new RuntimeException("Error 123"))
    monumentQuery.byMonumentTemplate(date = None) returns monuments

    val stat = new Statistics(contest, None, monumentQuery, imageQuery, bot)

    stat.gatherData() must throwA[RuntimeException].await
  }
}

