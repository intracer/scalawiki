package org.scalawiki.wiremock

class LoginWireMockSpec extends BaseWireMockSpec {

  val (user, password) = ("userName", "secret")
  val loginAction = Map("action" -> "login", "format" -> "json", "lgname" -> user, "lgpassword" -> password)

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

  "login" should {
    "succesfully login IlyaBot" in {
      stubOk(loginAction, needToken)
      stubOk(loginAction ++ Map("lgtoken" -> "token-value+\\"), loginSuccess)

      val result = login(getBot)
      result === "Success"
    }
  }

  "login" should {
    "reject login IlyaBot with wrong passwd" in {
      stubOk(loginAction, needToken)
      stubOk(loginAction ++ Map("lgtoken" -> "token-value+\\"), wrongPass)

      val result = login(getBot, "IlyaBot", "wrong")
      result === "WrongPass"
    }
  }

  "login" should {
    "reject login of wrong user" in {
      stubOk(loginAction, needToken)
      stubOk(loginAction ++ Map("lgtoken" -> "token-value+\\"), notExists)
      val result = login(getBot, "IlyaBotNotExistent", "wrong")
      result === "NotExists"
    }
  }

}
