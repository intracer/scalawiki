package org.scalawiki

class LoginIntegrationSpec extends BaseIntegrationSpec {

  "login" should {
     "succesfully login IlyaBot" in {
       val result = login(getUkWikiBot)
       result === "Success"
     }
  }

  "login" should {
    "reject login IlyaBot with wrong passwd" in {
      val result = login(getUkWikiBot, "IlyaBot", "wrong")
      result === "WrongPass"
    }
  }

  "login" should {
    "reject login of wrong user" in {
      val result = login(getUkWikiBot, "IlyaBotNotExistent", "wrong")
      result === "NotExists"
    }
  }

}
