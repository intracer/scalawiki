package org.scalawiki.edit

import org.scalawiki.MwBot
import org.scalawiki.util.{HttpStub, MockBotSpec, TestUtils}
import org.specs2.mutable.Specification

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters._

class PageUpdaterSpec extends Specification with MockBotSpec {

  // one spec at a time: the mock bot's HttpStub queue is order-sensitive
  sequential

  import ExecutionContext.Implicits.global

  /** A no-op task over the given pages (or `n` synthetic "Page k" ones). */
  class FakeTask(val titles: Seq[String]) extends PageUpdateTask {
    def this(n: Int) = this((1 to n).map("Page " + _))
    override def host: String = "uk.wikipedia.org"
    override def updatePage(title: String, text: String): (String, String) =
      (text + " [rated]", s"rated $title")
  }

  /** [[PageUpdater]] whose per-page work stands in for a real HTTP round trip
    * against a connection pool that rejects any request beyond `maxOpen` already
    * in flight — exactly what Pekko HTTP's `max-open-requests` buffer does, and
    * exactly what the old `update()` tripped by launching one future per page at
    * once. Records the peak concurrency it actually saw and which pages finished.
    */
  class PooledPageUpdater(task: PageUpdateTask, maxOpen: Int)
      extends PageUpdater(task) {

    private val inFlight = new AtomicInteger(0)
    val peakInFlight = new AtomicInteger(0)
    private val finished = new ConcurrentLinkedQueue[String]()

    def updatedPages: Seq[String] = finished.asScala.toSeq

    override def updatePage(title: String): Future[Any] = {
      val now = inFlight.incrementAndGet()
      peakInFlight.updateAndGet(p => math.max(p, now))
      if (now > maxOpen) {
        inFlight.decrementAndGet()
        Future.failed(
          new RuntimeException(
            s"Exceeded configured max-open-requests value of [$maxOpen]"
          )
        )
      } else {
        Future {
          Thread.sleep(1) // hold the "connection" for a beat
          finished.add(title)
          inFlight.decrementAndGet()
          "Success"
        }
      }
    }
  }

  "PageUpdater.update" should {

    "update every page when there are far more pages than the connection pool holds" in {
      // 1500 list pages, a pool that (like the real one) tolerates 2 in flight.
      // The pre-fix update() fired all 1500 at once => ~1498 BufferOverflow
      // failures and the run died after a handful of edits.
      val n = 1500
      val updater = new PooledPageUpdater(new FakeTask(n), maxOpen = 2)

      val results = Await.result(updater.update(), 60.seconds)

      updater.updatedPages must haveSize(n)
      updater.updatedPages.toSet must haveSize(n) // no page done twice or skipped
      results.count(_._2.isSuccess) === n
      updater.peakInFlight.get must beLessThanOrEqualTo(1)
    }

    "process the pages in order" in {
      val updater = new PooledPageUpdater(new FakeTask(50), maxOpen = 2)
      Await.result(updater.update(), 30.seconds)
      updater.updatedPages === (1 to 50).map("Page " + _)
    }

    "carry on after a page fails instead of aborting the batch" in {
      val updater = new PageUpdater(new FakeTask(10)) {
        override def updatePage(title: String): Future[Any] =
          if (title == "Page 5") Future.failed(new RuntimeException("boom"))
          else Future.successful("Success")
      }

      val results = Await.result(updater.update(), 10.seconds)

      results must haveSize(10)
      results.count(_._2.isSuccess) === 9
      results.collect { case (t, o) if o.isFailure => t } === Seq("Page 5")
    }

    "drive a real pageText + edit round trip for every page (mock bot)" in {
      val siteInfo =
        TestUtils.resourceAsString("/org/scalawiki/ukwiki_siteinfo.json")
      val token =
        """{"batchcomplete":"","query":{"tokens":{"csrftoken":"cafebabe+\\"}}}"""
      def ok(title: String) =
        s"""{"edit":{"result":"Success","pageid":1,"title":"$title","contentmodel":"wikitext"}}"""
      def editStub(title: String, text: String) =
        HttpStub(
          Map(
            "assert" -> "bot",
            "format" -> "json",
            "text" -> text,
            "token" -> "cafebabe+\\",
            "bot" -> "x",
            "title" -> title,
            "action" -> "edit",
            "summary" -> s"rated $title"
          ),
          ok(title)
        )

      val mockBot = getBot(
        HttpStub(Map("title" -> "P1", "action" -> "raw"), "a", "/w/index.php"),
        HttpStub(Map("action" -> "query", "meta" -> "siteinfo"), siteInfo),
        HttpStub(Map("action" -> "query", "meta" -> "tokens"), token),
        editStub("P1", "a [rated]"),
        HttpStub(Map("title" -> "P2", "action" -> "raw"), "b", "/w/index.php"),
        editStub("P2", "b [rated]"),
        HttpStub(Map("title" -> "P3", "action" -> "raw"), "c", "/w/index.php"),
        editStub("P3", "c [rated]")
      )

      val updater = new PageUpdater(new FakeTask(Seq("P1", "P2", "P3"))) {
        override implicit def bot: MwBot = mockBot
      }

      val results = Await.result(updater.update(), 30.seconds)

      results.map(_._1) === Seq("P1", "P2", "P3")
      results.count(_._2.isSuccess) === 3
    }
  }
}
