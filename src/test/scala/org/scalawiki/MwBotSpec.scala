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



