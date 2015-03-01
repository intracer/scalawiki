package org.scalawiki

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import org.scalawiki.util.{Command, TestHttpClient}
import org.specs2.mutable.Specification

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

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


  "get revisions text" should {
    "return a page text" in {
      val pageText1 = "some vandalism"
      val pageText2 = "more vandalism"

      val response =
        s"""{"query":{"pages":{
          |"569559":{"pageid":569559,"ns":1,"title":"Talk:Welfare reform", "revisions": [{"user": "u1", "comment":"c1", "*":"$pageText1"}]},
          |"4571809":{"pageid":4571809,"ns":2,"title":"User:Formator", "revisions": [{"user": "u2", "comment":"c2","*":"$pageText2"}]} }}}""".stripMargin

      val bot = getBot(new Command(
        Map(
          "pageids" -> "569559|4571809",
          "action" -> "query",
          "prop" -> "revisions",
          "continue" -> "",
          "rvprop"->"content|user|comment"), response))

      val future = bot.pagesById(Set(569559, 4571809)).revisions(Set.empty, Set("content", "user", "comment"))
      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size(2)
     // result(0) === Page(569559, 1, "Talk:Welfare reform", Seq(Revision("u1","t1","c1",Some(pageText1))))
    //  result(1) === Page(4571809, 2, "User:Formator", Seq(Revision("u2","t2","c2",Some(pageText2))))
    }
  }

//  "login" should {
//    "login" in {
//      val user = "userName"
//      val password = "secret"
//
//      val response1 = """{"login":{"result":"NeedToken","token":"a504e9507bb8e8d7d3bf839ef096f8f7","cookieprefix":"ukwiki","sessionid":"37b1d67422436e253f5554de23ae0064"}}"""
//
//
//      val bot = getBot(new Command(Map("action" -> "login", "lgname" -> user, "lgpassword" -> password), response1))
//
//      val future = bot.login(user, password)
//      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
//      result === pageText  TODO
//    }
//  }

  // {"login":{"result":"NeedToken","token":"a504e9507bb8e8d7d3bf839ef096f8f7","cookieprefix":"ukwiki","sessionid":"37b1d67422436e253f5554de23ae0064"}}


  def getBot(commands: Command*) = {
    val http = new TestHttpClient(host, mutable.Queue(commands:_*))

    new MwBot(http, system, host)
  }
}



