package org.scalawiki

import org.scalawiki.util.{Command, MockBotSpec}
import org.specs2.mutable.Specification
import spray.util.pimpFuture

class LoginSpec extends Specification with MockBotSpec {

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
    "get token and login" in {
      val bot = getBot(
        new Command(loginAction, needToken),
        new Command(loginAction ++ Map("lgtoken" -> "token-value+\\"), loginSuccess)
      )

      bot.login(user, password).await === "Success"
    }
  }

  "return wrong password" in {
    val bot = getBot(
      new Command(loginAction, needToken),
      new Command(loginAction ++ Map("lgtoken" -> "token-value+\\"), wrongPass)
    )

    bot.login(user, password).await === "WrongPass"
  }

  "throttler" in {
    val bot = getBot(
      new Command(loginAction, needToken),
      new Command(loginAction ++ Map("lgtoken" -> "token-value+\\"), throttled)
    )

    bot.login(user, password).await === "Throttled"
  }
}
