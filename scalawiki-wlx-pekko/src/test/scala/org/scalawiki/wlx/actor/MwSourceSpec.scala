package org.scalawiki.wlx.actor

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Sink
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.prop.{Info, Prop}
import org.scalawiki.util.{HttpStub, MockBotSpec}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext

class MwSourceSpec extends AnyWordSpec with Matchers with ScalaFutures with MockBotSpec with BeforeAndAfterAll {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds))

  // IMPORTANT: share one bot instance across all tests to avoid multiple ActorSystem instances
  val bot = getBot()

  implicit val system: ActorSystem = bot.system
  implicit val ec: ExecutionContext = system.dispatcher

  override def afterAll(): Unit = {
    bot.system.terminate()
    super.afterAll()
  }

  val action: Action = Action(Query(Prop(Info())))

  "MwSource.pages" should {

    "emit one batch when there is no continuation" in {
      val response =
        """{
          |  "batchcomplete": "",
          |  "query": {
          |    "pages": {
          |      "1": { "pageid": 1, "ns": 0, "title": "Page1" }
          |    }
          |  }
          |}""".stripMargin

      val testBot = getBot(
        HttpStub(
          Map("action" -> "query", "prop" -> "info", "continue" -> ""),
          response
        )
      )

      val future = MwSource
        .pages(action, testBot)
        .runWith(Sink.seq)
        .map(_.flatten)

      val pages = future.futureValue
      pages should have size 1
      pages.head.title shouldBe "Page1"
    }

    "emit two batches when there is one continuation" in {
      val response1 =
        """{
          |  "query": {
          |    "pages": {
          |      "1": { "pageid": 1, "ns": 0, "title": "Page1" }
          |    }
          |  },
          |  "continue": { "continue": "||", "inccontinue": "abc" }
          |}""".stripMargin

      val response2 =
        """{
          |  "batchcomplete": "",
          |  "query": {
          |    "pages": {
          |      "2": { "pageid": 2, "ns": 0, "title": "Page2" }
          |    }
          |  }
          |}""".stripMargin

      val testBot = getBot(
        HttpStub(
          Map("action" -> "query", "prop" -> "info", "continue" -> ""),
          response1
        ),
        HttpStub(
          Map(
            "action" -> "query",
            "prop" -> "info",
            "continue" -> "||",
            "inccontinue" -> "abc"
          ),
          response2
        )
      )

      val future = MwSource
        .pages(action, testBot)
        .runWith(Sink.seq)
        .map(_.flatten)

      val pages = future.futureValue
      pages should have size 2
      pages.map(_.title) shouldBe Seq("Page1", "Page2")
    }
  }
}
