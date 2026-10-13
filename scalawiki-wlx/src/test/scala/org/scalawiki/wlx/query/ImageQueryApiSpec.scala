package org.scalawiki.wlx.query

import org.apache.pekko.event.NoLogging
import org.scalawiki.ActionBot
import org.scalawiki.dto.Page
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
}
