package org.scalawiki.query

import org.scalawiki.Timestamp
import org.scalawiki.dto.Page
import org.scalawiki.util.{MockBotSpec, Command}
import org.scalawiki.dto.Image
import org.specs2.mutable.Specification
import spray.util.pimpFuture

class PropImageInfoSpec extends Specification with MockBotSpec {

  "get image info in generator" should {
    "return a page text" in {

      val response1 =
        """  {
          |    "limits": {
          |      "categorymembers": 500
          |    },
          |    "query": {
          |      "pages": {
          |      "32885574": {
          |      "pageid": 32885574,
          |      "ns": 6,
          |      "title": "File:\"Dovbush-rocks\" 01.JPG",
          |      "imagerepository": "local",
          |      "imageinfo": [
          |    {
          |      "timestamp": "2014-05-20T20:54:33Z",
          |      "user": "Taras r",
          |      "size": 4270655,
          |      "width": 3648,
          |      "height": 2736,
          |      "url": "https://upload.wikimedia.org/wikipedia/commons/e/ea/%22Dovbush-rocks%22_01.JPG",
          |      "descriptionurl": "https://commons.wikimedia.org/wiki/File:%22Dovbush-rocks%22_01.JPG"
          |    }]}}},
          |    "continue": {
          |    "gcmcontinue": "file|44454d45524749373631312e4a50470a44454d45524749373631312e4a5047|32763876",
          |    "continue": "gcmcontinue||"}}
        """.stripMargin

      val response2 =
        """  {
          |    "query": {
          |      "pages": {
          |      "32885597": {
          |      "pageid": 32885597,
          |      "ns": 6,
          |      "title": "File:\"Dovbush-rocks\" 02.JPG",
          |      "imagerepository": "local",
          |      "imageinfo": [
          |    {
          |      "timestamp": "2014-05-20T20:55:12Z",
          |      "user": "Taras r",
          |      "size": 4537737,
          |      "width": 2736,
          |      "height": 3648,
          |      "url": "https://upload.wikimedia.org/wikipedia/commons/2/26/%22Dovbush-rocks%22_02.JPG",
          |      "descriptionurl": "https://commons.wikimedia.org/wiki/File:%22Dovbush-rocks%22_02.JPG"
          |    }]}}}}
          |    """.stripMargin


      val commands = Seq(
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "imageinfo", "iiprop" -> "timestamp|user|comment",
          "continue" -> ""), response1),
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "imageinfo", "iiprop" -> "timestamp|user|comment",
          "continue" -> "gcmcontinue||",
          "gcmcontinue" -> "file|44454d45524749373631312e4a50470a44454d45524749373631312e4a5047|32763876"),
          response2)
      )

      val bot = getBot(commands: _*)

      val future = bot.page("Category:SomeCategory")
        .imageInfoByGenerator("categorymembers", "cm", Set.empty, Set("timestamp", "user", "comment"))

      val result = future.await

      result must have size 2

      result(0) === Page(Some(32885574), 6, "File:\"Dovbush-rocks\" 01.JPG", Seq.empty,
        Seq(Image.basic("File:\"Dovbush-rocks\" 01.JPG", Some(Timestamp.parse("2014-05-20T20:54:33Z")), Some("Taras r"), Some(4270655), Some(3648), Some(2736),
          Some("https://upload.wikimedia.org/wikipedia/commons/e/ea/%22Dovbush-rocks%22_01.JPG"),
          Some("https://commons.wikimedia.org/wiki/File:%22Dovbush-rocks%22_01.JPG"), Some(32885574))))
      result(1) === Page(Some(32885597), 6, "File:\"Dovbush-rocks\" 02.JPG", Seq.empty,
        Seq(Image.basic("File:\"Dovbush-rocks\" 02.JPG", Some(Timestamp.parse("2014-05-20T20:55:12Z")), Some("Taras r"), Some(4537737), Some(2736), Some(3648),
          Some("https://upload.wikimedia.org/wikipedia/commons/2/26/%22Dovbush-rocks%22_02.JPG"),
          Some("https://commons.wikimedia.org/wiki/File:%22Dovbush-rocks%22_02.JPG"), Some(32885597))))
    }
  }
}
