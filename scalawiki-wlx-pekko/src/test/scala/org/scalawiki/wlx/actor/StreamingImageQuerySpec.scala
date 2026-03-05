package org.scalawiki.wlx.actor

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Sink
import org.scalawiki.{MwBot, MwBotImpl}
import org.scalawiki.dto.Site
import org.scalawiki.util.{HttpStub, MockBotSpec, TestHttpClient}
import org.scalawiki.wlx.dto.Contest
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext

class StreamingImageQuerySpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with MockBotSpec
    with BeforeAndAfterAll {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds))

  val bot: MwBot = new MwBotImpl(
    Site.host(host),
    new TestHttpClient(host, Seq.empty),
    ActorSystem("StreamingImageQuerySpec")
  )

  implicit val system: ActorSystem  = bot.system
  implicit val ec: ExecutionContext = system.dispatcher

  override def afterAll(): Unit = {
    bot.system.terminate()
    super.afterAll()
  }

  val contest: Contest = Contest.WLMUkraine(2025)

  "StreamingImageQuery.source" should {

    "return Source.empty when fileTemplate is None" in {
      val noTemplateContest = contest.copy(uploadConfigs = Seq.empty)
      val future = StreamingImageQuery
        .source(noTemplateContest, bot)
        .runWith(Sink.seq)

      val images = future.futureValue
      images shouldBe empty
    }

    "complete stream with a file page response" in {
      val pageContent =
        """{{Monument Ukraine
          || 1 = 01-101-0001
          |}}
          |{{Information
          || author = [[User:TestUser|TestUser]]
          |}}""".stripMargin

      val response =
        s"""{
           |  "batchcomplete": "",
           |  "query": {
           |    "pages": {
           |      "100": {
           |        "pageid": 100,
           |        "ns": 6,
           |        "title": "File:Test monument.jpg",
           |        "imageinfo": [
           |          {
           |            "timestamp": "2025-06-01T12:00:00Z",
           |            "user": "TestUser",
           |            "size": 123456,
           |            "width": 1024,
           |            "height": 768,
           |            "url": "https://upload.wikimedia.org/wikipedia/commons/t/te/Test_monument.jpg",
           |            "descriptionurl": "https://commons.wikimedia.org/wiki/File:Test_monument.jpg"
           |          }
           |        ],
           |        "revisions": [
           |          {
           |            "revid": 200,
           |            "user": "TestUser",
           |            "userid": 42,
           |            "timestamp": "2025-06-01T12:00:00Z",
           |            "*": ${ujson(pageContent)}
           |          }
           |        ]
           |      }
           |    }
           |  }
           |}""".stripMargin

      val stubBot = new MwBotImpl(
        Site.host(host),
        new TestHttpClient(
          host,
          Seq(
            HttpStub(
              Map(
                "action"       -> "query",
                "generator"    -> "embeddedin",
                "geititle"     -> "Template:Monument Ukraine",
                "geinamespace" -> "6",
                "geilimit"     -> "50",
                "prop"         -> "info|revisions|imageinfo",
                "rvprop"       -> "content|ids|timestamp|user|userid",
                "iiprop"       -> "timestamp|user|size|url",
                "continue"     -> ""
              ),
              response
            )
          )
        ),
        bot.system
      )

      val future = StreamingImageQuery
        .source(contest, stubBot)
        .runWith(Sink.seq)

      val images = future.futureValue
      images should not be empty
      val image = images.head
      image.title shouldBe "File:Test monument.jpg"
      image.pageId shouldBe Some(100L)
      image.monumentIds shouldBe List("01-101-0001")
      image.author shouldBe Some("TestUser")
      image.width shouldBe Some(1024)
      image.height shouldBe Some(768)
      image.size shouldBe Some(123456L)
    }
  }

  private def ujson(text: String): String = {
    val escaped = text
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
    s""""$escaped""""
  }
}
