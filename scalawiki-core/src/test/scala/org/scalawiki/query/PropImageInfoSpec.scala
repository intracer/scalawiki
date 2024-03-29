package org.scalawiki.query

import java.time.{ZoneOffset, ZonedDateTime}
import org.scalawiki.Timestamp
import org.scalawiki.dto.Page
import org.scalawiki.util.{HttpStub, MockBotSpec}
import org.scalawiki.dto.Image
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.{PageIdsParam, Query}
import org.scalawiki.dto.cmd.query.prop.{ImageInfo, Prop}
import org.scalawiki.dto.cmd.query.prop.iiprop.{IiProp, Metadata}
import org.scalawiki.util.TestUtils.resourceAsString
import org.specs2.mutable.Specification
import spray.util.pimpFuture

import scala.concurrent.ExecutionContext.Implicits.global

class PropImageInfoSpec extends Specification with MockBotSpec {

  def response1(generatorPrefix: String = "cm") =
    s"""  {
       |    "query": {
       |      "pages": {
       |      "32885574": {
       |      "pageid": 32885574,
       |      "ns": 6,
       |      "title": "File:Dovbush-rocks 01.JPG",
       |      "imagerepository": "local",
       |      "imageinfo": [
       |    {
       |      "timestamp": "2014-05-20T20:54:33Z",
       |      "user": "Taras r",
       |      "size": 4270655,
       |      "width": 3648,
       |      "height": 2736,
       |      "url": "https://upload.wikimedia.org/wikipedia/commons/e/ea/Dovbush-rocks_01.JPG",
       |      "descriptionurl": "https://commons.wikimedia.org/wiki/File:Dovbush-rocks_01.JPG"
       |    }]}}},
       |    "continue": {
       |    "g${generatorPrefix}continue": "file|44454d45524749373631312e4a50470a44454d45524749373631312e4a5047|32763876",
       |    "continue": "g${generatorPrefix}continue||"}}
    """.stripMargin

  val response2 =
    """  {
      |    "query": {
      |      "pages": {
      |      "32885597": {
      |      "pageid": 32885597,
      |      "ns": 6,
      |      "title": "File:Dovbush-rocks 02.JPG",
      |      "imagerepository": "local",
      |      "imageinfo": [
      |    {
      |      "timestamp": "2014-05-20T20:55:12Z",
      |      "user": "Taras r",
      |      "size": 4537737,
      |      "width": 2736,
      |      "height": 3648,
      |      "url": "https://upload.wikimedia.org/wikipedia/commons/2/26/Dovbush-rocks_02.JPG",
      |      "descriptionurl": "https://commons.wikimedia.org/wiki/File:Dovbush-rocks_02.JPG"
      |    }]}}}}
      |    """.stripMargin

  val page1 = Page(
    Some(32885574),
    Some(6),
    "File:Dovbush-rocks 01.JPG",
    Seq.empty,
    Seq(
      Image.basic(
        "File:Dovbush-rocks 01.JPG",
        Some(Timestamp.parse("2014-05-20T20:54:33Z")),
        Some("Taras r"),
        Some(4270655),
        Some(3648),
        Some(2736),
        Some(
          "https://upload.wikimedia.org/wikipedia/commons/e/ea/Dovbush-rocks_01.JPG"
        ),
        Some("https://commons.wikimedia.org/wiki/File:Dovbush-rocks_01.JPG"),
        Some(32885574)
      )
    )
  )

  val page2 = Page(
    Some(32885597),
    Some(6),
    "File:Dovbush-rocks 02.JPG",
    Seq.empty,
    Seq(
      Image.basic(
        "File:Dovbush-rocks 02.JPG",
        Some(Timestamp.parse("2014-05-20T20:55:12Z")),
        Some("Taras r"),
        Some(4537737),
        Some(2736),
        Some(3648),
        Some(
          "https://upload.wikimedia.org/wikipedia/commons/2/26/Dovbush-rocks_02.JPG"
        ),
        Some("https://commons.wikimedia.org/wiki/File:Dovbush-rocks_02.JPG"),
        Some(32885597)
      )
    )
  )

  "get image info in generator" should {
    "query by category members" in {

      val commands = Seq(
        HttpStub(
          Map(
            "action" -> "query",
            "generator" -> "categorymembers",
            "gcmtitle" -> "Category:SomeCategory",
            "gcmlimit" -> "max",
            "prop" -> "imageinfo",
            "iiprop" -> "timestamp|user|comment",
            "continue" -> ""
          ),
          response1("cm")
        ),
        HttpStub(
          Map(
            "action" -> "query",
            "generator" -> "categorymembers",
            "gcmtitle" -> "Category:SomeCategory",
            "gcmlimit" -> "max",
            "prop" -> "imageinfo",
            "iiprop" -> "timestamp|user|comment",
            "continue" -> "gcmcontinue||",
            "gcmcontinue" -> "file|44454d45524749373631312e4a50470a44454d45524749373631312e4a5047|32763876"
          ),
          response2
        )
      )

      val bot = getBot(commands: _*)

      val future = bot
        .page("Category:SomeCategory")
        .imageInfoByGenerator(
          "categorymembers",
          "cm",
          Set.empty,
          Set("timestamp", "user", "comment")
        )
        .map(_.toSeq)

      val result = future.await

      result must have size 2

      result(0) === page1
      result(1) === page2
    }

    "query by page images" in {

      val commands = Seq(
        HttpStub(
          Map(
            "action" -> "query",
            "generator" -> "images",
            "titles" -> "Commons:SomePage",
            "gimlimit" -> "max",
            "prop" -> "imageinfo",
            "iiprop" -> "timestamp|user|comment",
            "continue" -> ""
          ),
          response1("im")
        ),
        HttpStub(
          Map(
            "action" -> "query",
            "generator" -> "images",
            "titles" -> "Commons:SomePage",
            "gimlimit" -> "max",
            "prop" -> "imageinfo",
            "iiprop" -> "timestamp|user|comment",
            "continue" -> "gimcontinue||",
            "gimcontinue" -> "file|44454d45524749373631312e4a50470a44454d45524749373631312e4a5047|32763876"
          ),
          response2
        )
      )

      val bot = getBot(commands: _*)

      val future = bot
        .page("Commons:SomePage")
        .imageInfoByGenerator(
          "images",
          "im",
          Set.empty,
          Set("timestamp", "user", "comment")
        )
        .map(_.toSeq)

      val result = future.await

      result must have size 2

      result(0) === page1
      result(1) === page2
    }

    "get metadata" in {
      val s = resourceAsString("/org/scalawiki/query/imageMetadata.json")

      val commands = Seq(
        HttpStub(
          Map(
            "action" -> "query",
            "format" -> "json",
            "prop" -> "imageinfo",
            "pageids" -> "58655318",
            "iiprop" -> "metadata",
            "continue" -> ""
          ),
          s
        )
      )

      val bot = getBot(commands: _*)

      val future = bot.run(
        Action(
          Query(PageIdsParam(Seq(58655318)), Prop(ImageInfo(IiProp(Metadata))))
        )
      )
      val result = future.await

      result must have size 1
      val page = result.head

      page.images must have size 1
      val image = page.images.head

      image.metadata.isDefined === true
      val metadata = image.metadata.get
      metadata.camera === Some("Canon EOS 450D")
      metadata.date === Some(
        ZonedDateTime.of(2017, 4, 22, 12, 26, 44, 0, ZoneOffset.UTC)
      )
    }
  }
}
