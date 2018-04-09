package org.scalawiki.wiremock

class LoginWireMockSpec extends BaseWireMockSpec {

  val (user, absentUser) = ("secretPassword", "wrongPassword")
  val (password, wrongPassword) = ("userName", "absentUSer")

  def withCredentials(user: String, password: String) =
    Map("action" -> "login", "format" -> "json", "lgname" -> user, "lgpassword" -> password)

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

  val wrongPassResult = result("WrongPass")
  val notExistsResult = result("NotExists")
  val throttledResult = result("Throttled")

  "login" should {
    "succesfully login IlyaBot" in {
      val loginAction = withCredentials(user, password)
      stubOk(loginAction, needToken)
      stubOk(loginAction ++ Map("lgtoken" -> "token-value+\\"), loginSuccess)

      val result = login(getBot, user, password)
      result === "Success"
    }
  }

  "login" should {
    "reject login IlyaBot with wrong passwd" in {
      val loginAction = withCredentials(user, wrongPassword)
      stubOk(loginAction, needToken)
      stubOk(loginAction ++ Map("lgtoken" -> "token-value+\\"), wrongPassResult)

      val result = login(getBot, "IlyaBot", "wrong")
      result === "WrongPass"
    }
  }

  "login" should {
    "reject login of wrong user" in {
      val loginAction = withCredentials(absentUser, wrongPassword)
      stubOk(loginAction, needToken)
      stubOk(loginAction ++ Map("lgtoken" -> "token-value+\\"), notExistsResult)
      val result = login(getBot, absentUser, wrongPassword)
      result === "NotExists"
    }
  }

}
