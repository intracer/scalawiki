package org.scalawiki

import akka.actor.ActorSystem
import org.scalawiki.util.{Command, TestHttpClient}
import org.specs2.mutable.Specification
import spray.util.pimpFuture

import scala.collection.mutable

class MwBotSpec extends Specification {

  val host = "uk.wikipedia.org"

  private val system: ActorSystem = ActorSystem()

  "get page text" should {
    "return a page text" in {
      val pageText = "some vandalism"

      val bot = getBot(new Command(Map("title" -> "PageTitle", "action" -> "raw"), pageText, "/w/index.php"))

      val result = bot.pageText("pageTitle").await
      result === pageText
    }
  }

  "get missing page text" should {
    "return error" in {

      val bot = getBot(new Command(Map("title" -> "PageTitle", "action" -> "raw"), null, "/w/index.php"))

      val result = bot.pageText("pageTitle").await
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
//      val result = bot.login(user, password).await
//      result === pageText  TODO
//    }
//  }

  // {"login":{"result":"NeedToken","token":"a504e9507bb8e8d7d3bf839ef096f8f7","cookieprefix":"ukwiki","sessionid":"37b1d67422436e253f5554de23ae0064"}}


  def getBot(commands: Command*) = {
    val http = new TestHttpClient(host, mutable.Queue(commands:_*))

    new MwBotImpl(host, http, system)
  }
}



