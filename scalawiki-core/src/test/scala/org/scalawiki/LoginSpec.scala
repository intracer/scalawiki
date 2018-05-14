package org.scalawiki

import akka.http.scaladsl.model.{ContentType, HttpCharsets, MediaTypes}
import org.scalawiki.dto.MwException
import org.scalawiki.util.{HttpStub, MockBotSpec, TestUtils}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.ThrownExpectations
import org.specs2.mutable.Specification

class LoginSpec extends Specification with MockBotSpec with ThrownExpectations {

  type EE = ExecutionEnv

  val needToken =
    """{ "login": {
                "result": "NeedToken",
                "token": "token-value+\\",
                "cookieprefix":"enwiki",
                "sessionid":"sessionid-value"
                }
           }"""

  val loginSuccess =
    """{ "login": {
                "result": "Success",
                "lguserid":678,
                "lgusername":"IlyaBot",
                "lgtoken":"token-value",
                "cookieprefix":"enwiki",
                "sessionid":"sessionid-value"
                }
           }"""

  def result(code: String) = s"""{"login":{"result":"$code"}}"""

  val wrongPass = result("WrongPass")
  val notExists = result("NotExists")
  val throttled = result("Throttled")

  val (user, password) = ("userName", "secret")
  val loginAction = Map("action" -> "login", "format" -> "json", "lgname" -> user, "lgpassword" -> password)

  "login" should {
    "get token and login" in { implicit ee: EE =>
      val bot = getBot(
        HttpStub(loginAction, needToken),
        HttpStub(loginAction ++ Map("lgtoken" -> "token-value+\\"), loginSuccess)
      )

      bot.login(user, password).map(_ === "Success").await
    }
  }

  "return wrong password" in { implicit ee: EE =>
    val bot = getBot(
      HttpStub(loginAction, needToken),
      HttpStub(loginAction ++ Map("lgtoken" -> "token-value+\\"), wrongPass)
    )

    bot.login(user, password).map(_ === "WrongPass").await
  }

  "throttler" in { implicit ee: EE =>
    val bot = getBot(
      HttpStub(loginAction, needToken),
      HttpStub(loginAction ++ Map("lgtoken" -> "token-value+\\"), throttled)
    )

    bot.login(user, password).map(_ === "Throttled").await
  }

  "err503" in { implicit ee: EE =>

    val err = TestUtils.resourceAsString("/org/scalawiki/Wikimedia Error.html")

    val bot = getBot(
      HttpStub(loginAction, err, contentType = ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`))
    )

    val f = bot.login(user, password)

    f.failed.map {
      case e: MwException =>
        e.info must contain("Error: 503, Service Unavailable")
    }.await

  }
}
