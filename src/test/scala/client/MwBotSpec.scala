package client

import akka.actor.ActorSystem
import client.dto.{Revision, PageQuery, Page}
import java.util.concurrent.TimeUnit
import org.specs2.mutable.Specification
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import client.util.{Command, TestHttpClient}
import scala.collection.mutable

class MwBotSpec extends Specification {

  val host = "uk.wikipedia.org"

  private val system: ActorSystem = ActorSystem()

  "get page text" should {
    "return a page text" in {
      val pageText = "some vandalism"

      val bot = getBot(new Command(Map("title" -> "PageTitle", "action" -> "raw"), pageText, "/w/index.php"))

      val future = bot.pageText("pageTitle")
      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result === pageText
    }
  }

  "get missing page text" should {
    "return error" in {

      val bot = getBot(new Command(Map("title" -> "PageTitle", "action" -> "raw"), null, "/w/index.php"))

      val future = bot.pageText("pageTitle")
      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result === ""   // TODO error
    }
  }



  "get no embedded in" should {
    "return empty seq" in {
      val queryType = "embeddedin"

      //      val resonse = {"query-continue":{"embeddedin":{"eicontinue":"10|Stub|6674690"}},"query":{"embeddedin":[{"pageid":569559,"ns":1,"title":"Talk:Welfare reform"},{"pageid":2581310,"ns":2,"title":"User:Rpyle731/sandbox/archive1"},{"pageid":3954860,"ns":3,"title":"User talk:PBS/Archive 6"},{"pageid":4571809,"ns":2,"title":"User:Formator"},{"pageid":5024711,"ns":3,"title":"User talk:Rauterkus"}]}}

      val response = """{"query":{"embeddedin":[]}}"""

      val bot = getBot(new Command(Map("action" -> "query", "list" -> queryType, "eilimit" -> "max", "eititle" -> "Template:SomeTemplate", "continue" -> ""), response))

      val future = bot.whatTranscludesHere(PageQuery.byTitle("Template:SomeTemplate"))
      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 0
    }
  }

  "get one embedded in" should {
    "return one embedded in" in {
      val queryType = "embeddedin"

//      val resonse = {"query-continue":{"embeddedin":{"eicontinue":"10|Stub|6674690"}},"query":{"embeddedin":[{"pageid":569559,"ns":1,"title":"Talk:Welfare reform"},{"pageid":2581310,"ns":2,"title":"User:Rpyle731/sandbox/archive1"},{"pageid":3954860,"ns":3,"title":"User talk:PBS/Archive 6"},{"pageid":4571809,"ns":2,"title":"User:Formator"},{"pageid":5024711,"ns":3,"title":"User talk:Rauterkus"}]}}

      val response = """{"limits": {"embeddedin": 500}, "query":{"embeddedin":[{"pageid":569559,"ns":1,"title":"Talk:Welfare reform"}]}}"""

      val bot = getBot(new Command(Map("action" -> "query", "list" -> queryType, "eilimit" -> "max", "eititle" -> "Template:SomeTemplate", "continue" -> ""), response))

      val future = bot.whatTranscludesHere(PageQuery.byTitle("Template:SomeTemplate"))
      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size(1)
      result(0) === Page(569559, 1, "Talk:Welfare reform")
    }
  }

  "get two embedded in" should {
    "return two embedded in" in {
      val queryType = "embeddedin"

      val response =
        """{"limits": {"embeddedin": 500}, "query":{"embeddedin":[
          |{"pageid":569559,"ns":1,"title":"Talk:Welfare reform"},
          |{"pageid":4571809,"ns":2,"title":"User:Formator"}]}}""".stripMargin

      val bot = getBot(new Command(Map("action" -> "query", "list" -> queryType, "eilimit" -> "max", "eititle" -> "Template:SomeTemplate", "continue" -> ""), response))

      val future = bot.whatTranscludesHere(PageQuery.byTitle("Template:SomeTemplate"))
      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size(2)
      result(0) === Page(569559, 1, "Talk:Welfare reform")
      result(1) === Page(4571809, 2, "User:Formator")
    }
  }

  "get embedded in with continue" should {
    "return one embedded in" in {
      val queryType = "embeddedin"

      //      val resonse = {"query-continue":{"embeddedin":{"eicontinue":"10|Stub|6674690"}},"query":{"embeddedin":[{"pageid":569559,"ns":1,"title":"Talk:Welfare reform"},{"pageid":2581310,"ns":2,"title":"User:Rpyle731/sandbox/archive1"},{"pageid":3954860,"ns":3,"title":"User talk:PBS/Archive 6"},{"pageid":4571809,"ns":2,"title":"User:Formator"},{"pageid":5024711,"ns":3,"title":"User talk:Rauterkus"}]}}

      val response1 = """{"query":{"embeddedin":[{"pageid":569559,"ns":1,"title":"Talk:Welfare reform"}]}, "continue":{"continue":"-||","eicontinue":"10|Stub|6674690"}}"""
      val response2 = """{"limits": {"embeddedin": 500}, "query":{"embeddedin":[ {"pageid":4571809,"ns":2,"title":"User:Formator"}]}}"""

      val commands = Seq(
        new Command(Map("action" -> "query", "list" -> queryType, "eilimit" -> "max", "eititle" -> "Template:SomeTemplate", "continue" -> ""), response1),
        new Command(Map("action" -> "query", "list" -> queryType, "eilimit" -> "max", "eititle" -> "Template:SomeTemplate", "continue" -> "-||", "eicontinue" -> "10|Stub|6674690"), response2)
      )

      val bot = getBot(commands:_*)

      val future = bot.whatTranscludesHere(PageQuery.byTitle("Template:SomeTemplate"))
      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size(2)
      result(0) === Page(569559, 1, "Talk:Welfare reform")
      result(1) === Page(4571809, 2, "User:Formator")
    }
  }

  "get revisions text" should {
    "return a page text" in {
      val pageText1 = "some vandalism"
      val pageText2 = "more vandalism"

      val response =
        s"""{"limits": {"embeddedin": 500}, "query":{"pages":{
          |"569559":{"pageid":569559,"ns":1,"title":"Talk:Welfare reform", "revisions": [{"user": "u1", "timestamp" : "t1", "comment":"c1", "*":"$pageText1"}]},
          |"4571809":{"pageid":4571809,"ns":2,"title":"User:Formator", "revisions": [{"user": "u2", "timestamp" : "t2", "comment":"c2","*":"$pageText2"}]} }}}""".stripMargin

      val bot = getBot(new Command(
        Map(
          "pageids" -> "569559|4571809",
          "action" -> "query",
          "prop" -> "revisions",
          "continue" -> "",
          "rvprop"->"content|timestamp|user|comment"), response))

      val future = bot.revisions(PageQuery.byIds(Set(569559, 4571809)), Set.empty, Set("content", "timestamp", "user", "comment"))
      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size(2)
      result(0) === Page(569559, 1, "Talk:Welfare reform", Seq(Revision("u1","t1","c1",pageText1)))
      result(1) === Page(4571809, 2, "User:Formator", Seq(Revision("u2","t2","c2",pageText2)))
    }
  }

  // {"login":{"result":"NeedToken","token":"a504e9507bb8e8d7d3bf839ef096f8f7","cookieprefix":"ukwiki","sessionid":"37b1d67422436e253f5554de23ae0064"}}


  def getBot(commands: Command*) = {
    val http = new TestHttpClient(host, mutable.Queue(commands:_*))

    new MwBot(http, system, host)

  }
}



