package org.scalawiki.wlx.query

import org.apache.pekko.event.NoLogging
import org.scalawiki.ActionBot
import org.scalawiki.dto.{Image, Page, Revision}
import org.scalawiki.dto.cmd.Action
import org.scalawiki.util.AsyncGate
import org.scalawiki.wlx.dto.Contest
import org.specs2.mutable.Specification

import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ImageQueryApiSpec extends Specification {
  class ControlledBot extends ActionBot {
    val pending = new LinkedBlockingQueue[(Seq[Long], Promise[Iterable[Page]])]()
    val active = new AtomicInteger()
    val peak = new AtomicInteger()
    val calls = new AtomicInteger()
    override def log = NoLogging
    override def run(action: Action, context: Map[String, String], limit: Option[Long]): Future[Iterable[Page]] = {
      val ids = action.pairs.find(_._1 == "pageids").get._2.split("\\|").map(_.toLong).toSeq
      val count = active.incrementAndGet()
      peak.updateAndGet(p => math.max(p, count))
      calls.incrementAndGet()
      val promise = Promise[Iterable[Page]]()
      pending.add(ids -> promise)
      promise.future
    }
    def take() = {
      val task = pending.poll(5, TimeUnit.SECONDS)
      require(task != null, "expected a scheduled batch")
      active.decrementAndGet()
      task
    }
  }

  "image metadata batching" should {
    "share its concurrency budget across queries and fetch every id exactly once" in {
      val bot = new ControlledBot
      val gate = new AsyncGate(2)
      val first = new ImageQueryApi(bot, gate).imagesWithTemplateByIds(Contest.WLMUkraine(2012), (1L to 7000L).toSet)
      val second = new ImageQueryApi(bot, gate).imagesWithTemplateByIds(Contest.WLMUkraine(2013), (7001L to 12000L).toSet)
      val requested = Vector.newBuilder[Long]
      (1 to 240).foreach { _ =>
        val (ids, promise) = bot.take()
        requested ++= ids
        promise.success(Nil)
      }
      Await.result(Future.sequence(Seq(first, second)), 10.seconds)
      val ids = requested.result()
      ids.size must_== 12000
      ids.toSet must_== (1L to 12000L).toSet
      bot.peak.get must beLessThanOrEqualTo(2)
    }

    "stop scheduling later windows after a batch fails" in {
      val bot = new ControlledBot
      val result = new ImageQueryApi(bot, new AsyncGate(2))
        .imagesWithTemplateByIds(Contest.WLMUkraine(2012), (1L to 7000L).toSet)
      bot.take()._2.failure(new RuntimeException("network failed"))
      bot.take()._2.success(Nil)
      Await.result(result, 5.seconds) must throwA[RuntimeException]("network failed")
      bot.calls.get must_== 2
    }
  }

  "a file whose page names no author" should {
    def version(uploader: String) = Image.basic("File:Театр.jpg", None, Some(uploader), None, None, None, None, None, Some(1L))

    // the image query sees only the latest version; the history query lists all of them, newest first
    class HistoryBot(text: String, history: Seq[String]) extends ActionBot {
      val historyRequests = new AtomicInteger()
      override def log = NoLogging
      override def run(action: Action, context: Map[String, String], limit: Option[Long]): Future[Iterable[Page]] =
        Future.successful {
          if (action.pairs.toMap.get("iilimit").contains("max")) {
            historyRequests.incrementAndGet()
            Seq(Page(Some(1L), Some(6), "File:Театр.jpg", images = history.map(version)))
          } else
            Seq(Page(Some(1L), Some(6), "File:Театр.jpg", Seq(Revision.one(text)), Seq(version(history.head))))
        }
    }

    def author(bot: ActionBot) =
      Await.result(new ImageQueryApi(bot).imagesWithTemplateByIds(Contest.WLMUkraine(2012), Set(1L)), 5.seconds).head.author

    "be credited to the original uploader, not to whoever uploaded the latest version" in {
      author(new HistoryBot("{{Зображення|Автор=Я}}", Seq("Andrew J.Kurbiko", "Tttaaannne", "Бахтін"))) === Some("Бахтін")
    }

    "not be credited to a bot that uploaded it" in {
      author(new HistoryBot("{{Art Photo|photographer=}}", Seq("IlyaBot"))) === None
    }

    "not need the history when the page names the author" in {
      val bot = new HistoryBot("{{Зображення|Автор=[[Користувач:Бахтін|Бахтін]]}}", Seq("Andrew J.Kurbiko"))
      author(bot) === Some("Бахтін")
      bot.historyRequests.get === 0
    }
  }
}
