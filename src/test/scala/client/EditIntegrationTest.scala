package client

import client.util.BaseIntegrationSpec

class EditIntegrationTest extends BaseIntegrationSpec {

  "test" should {
    "edit with csrf token" in {

      val bot = getUkWikiBot
      val result = login(bot)
      result === "Success"

      val token = await(bot.getTokens)
      token must not be empty

      //val token = "f155c84991f7e19b8feb06c4fc78f6ea+\\"

      //await(bot.page("User:IlyaBot/test").editToken).head.edittoken.get

      token must not be empty

      val response = await(bot.page("User:IlyaBot/test").edit("text1", "comment", Some(token)))
      response === "Success"

    }
  }



}
