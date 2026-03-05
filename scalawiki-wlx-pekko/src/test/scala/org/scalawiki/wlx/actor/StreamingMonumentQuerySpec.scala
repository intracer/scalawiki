package org.scalawiki.wlx.actor

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Sink
import org.scalawiki.{MwBot, MwBotImpl}
import org.scalawiki.dto.Site
import org.scalawiki.util.{HttpStub, MockBotSpec, TestHttpClient}
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext

class StreamingMonumentQuerySpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with MockBotSpec
    with BeforeAndAfterAll {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds))

  // Dedicated ActorSystem for this spec so we can safely terminate it in afterAll
  // without affecting the shared MwBot.system used by other specs.
  val bot: MwBot = new MwBotImpl(
    Site.host(host),
    new TestHttpClient(host, Seq.empty),
    ActorSystem("StreamingMonumentQuerySpec")
  )

  implicit val system: ActorSystem = bot.system
  implicit val ec: ExecutionContext = system.dispatcher

  override def afterAll(): Unit = {
    bot.system.terminate()
    super.afterAll()
  }

  val contest: Contest = Contest.WLMUkraine(2025)

  "StreamingMonumentQuery.source" should {

    "complete stream with monument template pages" in {
      val pageContent =
        """{{ВЛП-рядок
          || ID = 01-101-0001
          || назва = Тестовий пам'ятник
          || рік = 1900
          || нас_пункт = Тест
          || адреса = вул. Тестова, 1
          || широта = 50.0
          || довгота = 30.0
          || охоронний номер = 1234
          || тип = місцевий
          || фото =
          || галерея =
          |}}""".stripMargin

      val response =
        s"""{
           |  "batchcomplete": "",
           |  "query": {
           |    "pages": {
           |      "42": {
           |        "pageid": 42,
           |        "ns": 0,
           |        "title": "Список пам'яток Тестового району",
           |        "revisions": [
           |          {
           |            "revid": 100,
           |            "user": "TestUser",
           |            "userid": 1,
           |            "timestamp": "2025-01-01T00:00:00Z",
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
                "action" -> "query",
                "generator" -> "embeddedin",
                "geititle" -> "Template:ВЛП-рядок",
                "geinamespace" -> "4|0",
                "geilimit" -> "100",
                "prop" -> "revisions",
                "rvprop" -> "content|ids|timestamp|user|userid",
                "continue" -> ""
              ),
              response
            )
          )
        ),
        bot.system
      )

      val future = StreamingMonumentQuery
        .source(contest, stubBot)
        .runWith(Sink.seq)

      val monuments = future.futureValue
      // Stream completes without error; template name mismatch means no monuments parsed
      monuments shouldBe Seq(
        Monument(
          page = "Список пам'яток Тестового району",
          id = "01-101-0001",
          name = "Тестовий пам'ятник",
          nameDetail = None,
          year = Some("1900"),
          description = None,
          article = None,
          city = Some("Тест"),
          cityType = None,
          place = Some("вул. Тестова, 1"),
          user = None,
          area = None,
          lat = Some("50.0"),
          lon = Some("30.0"),
          typ = Some("місцевий"),
          subType = None,
          photo = None,
          gallery = None,
          resolution = None,
          stateId = Some("1234"),
          contest = None,
          source = None,
          otherParams = Map(),
          listConfig = contest.uploadConfigs.headOption.map(_.listConfig)
        )
      )
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
