package client

import java.util.concurrent.TimeUnit

import org.scalawiki.dto.{ImageInfo, Page}
import client.util.{Command, MockBotSpec}
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class GeneratorSpec extends Specification with MockBotSpec {

  "get revisions text in generator" should {
    "return a page text" in {
      val pageText1 = "some vandalism"
      val pageText2 = "more vandalism"

      val response1 =
        s"""{"query":{"pages":{
          |"569559":{"pageid":569559,"ns":1,"title":"Talk:Welfare reform", "revisions": [{"user": "u1", "timestamp" : "t1", "comment":"c1", "*":"$pageText1"}] }}},
          | "continue":{"continue":"gcmcontinue||","gcmcontinue":"10|Stub|6674690"}}""".stripMargin

      val response2 =
        s"""{"query":{"pages":{
          |"4571809":{"pageid":4571809,"ns":2,"title":"User:Formator", "revisions": [{"user": "u2", "timestamp" : "t2", "comment":"c2","*":"$pageText2"}]} }}}""".stripMargin


      val commands = Seq(
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "revisions", "rvprop" -> "content|timestamp|user|comment",
          "continue" -> ""), response1),
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "revisions", "rvprop" -> "content|timestamp|user|comment",
          "continue" -> "gcmcontinue||", "gcmcontinue" -> "10|Stub|6674690"), response2)
      )

      val bot = getBot(commands: _*)

      val future = bot.page("Category:SomeCategory").revisionsByGenerator("categorymembers", "cm", Set.empty, Set("content", "timestamp", "user", "comment"))
      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 2
     // result(0) === Page(569559, 1, "Talk:Welfare reform", Seq(Revision("u1", "t1", "c1", Some(pageText1))))
     // result(1) === Page(4571809, 2, "User:Formator", Seq(Revision("u2", "t2", "c2", Some(pageText2))))
    }
  }

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
          |    "continue":{"gcmcontinue":"file|44454d45524749373631312e4a50470a44454d45524749373631312e4a5047|32763876","continue":"gcmcontinue||"}}
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
          "prop" -> "imageinfo", "iiprop" -> "content|timestamp|user|comment",
          "continue" -> ""), response1),
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "imageinfo", "iiprop" -> "content|timestamp|user|comment",
          "continue" -> "gcmcontinue||", "gcmcontinue" -> "file|44454d45524749373631312e4a50470a44454d45524749373631312e4a5047|32763876"), response2)
      )

      val bot = getBot(commands: _*)

      val future = bot.page("Category:SomeCategory").imageInfoByGenerator("categorymembers", "cm", Set.empty, Set("content", "timestamp", "user", "comment"))
      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 2
      result(0) === Page(32885574, 6, "File:\"Dovbush-rocks\" 01.JPG", Seq.empty, Seq(ImageInfo("2014-05-20T20:54:33Z", "Taras r", 4270655, 3648, 2736,
        "https://upload.wikimedia.org/wikipedia/commons/e/ea/%22Dovbush-rocks%22_01.JPG", "https://commons.wikimedia.org/wiki/File:%22Dovbush-rocks%22_01.JPG")))
      result(1) === Page(32885597, 6, "File:\"Dovbush-rocks\" 02.JPG", Seq.empty, Seq(ImageInfo("2014-05-20T20:55:12Z", "Taras r", 4537737, 2736, 3648,
        "https://upload.wikimedia.org/wikipedia/commons/2/26/%22Dovbush-rocks%22_02.JPG", "https://commons.wikimedia.org/wiki/File:%22Dovbush-rocks%22_02.JPG")))
    }
  }
}
