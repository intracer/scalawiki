package org.scalawiki.wlx.streams

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.apache.pekko.util.Timeout
import org.scalawiki.dto.Image
import org.specs2.mutable.Specification
import spray.util.pimpFuture

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class StreamsSpec extends Specification {
  val system = ActorSystem("wlm")
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = Timeout(5.seconds)

  private val years: Range.Inclusive = 2012 to 2026
  private val categoriesSource = Source(years)
//    .map(year => s"Category:Images from Wiki Loves Monuments $year in Ukraine")

  def image(n: Long, year: Int) = new Image(
    title = s"File:${year}_$n.jpg",
    author = Some(s"A$n"),
    monumentIds = Seq(s"M$n"),
    pageId = Some(year * 10 + n),
    year = Some(year)
  )

  def categoryMembers(year: Int) = Source(Seq(image(1, year)))

  def authorsStat(source: Source[Image, _]): Source[(String, List[Image]), _] = {
    def author(i: Image): String = i.author.getOrElse("")
    def reduce(l: (String, List[Image]), r: (String, List[Image])): (String, List[Image]) = {
      l._1 -> (l._2 ++ r._2)
    }

    source
      .groupBy(Integer.MAX_VALUE, author)
      .map(i => author(i) -> List(i))
      .reduce(reduce)
      .mergeSubstreams
  }

  private val allCategoryMembers = categoriesSource
    .mapAsync(parallelism = 4) { year =>
      categoryMembers(year).runWith(Sink.seq).map(members => year -> members)
    }

  "streams" should {
    "get year categories" in {
      val categories = categoriesSource.runWith(Sink.seq).await
      categories should_=== years // .map(year => s"Category:Images from Wiki Loves Monuments $year in Ukraine")
    }

    "get category members" in {
      val members = categoryMembers(2012).runWith(Sink.seq).await
      members should_=== Seq(image(1, 2012))
    }

    "get all category members" in {
      val members = allCategoryMembers.runWith(Sink.seq).await.toMap
      members should_=== years.map(year => year -> Seq(image(1, year))).toMap
    }

    "group by authors" in {
      val members = authorsStat(allCategoryMembers.mapConcat(_._2))
        .runWith(Sink.seq).await.toMap
        .view.mapValues(_.flatMap(_.pageId)).toMap
      members should_=== Map("A1" -> years.map(_ * 10L + 1).toList)
    }

  }

}
