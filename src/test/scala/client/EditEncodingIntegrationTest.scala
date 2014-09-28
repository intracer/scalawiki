package client

import client.util.BaseIntegrationSpec

class EditEncodingIntegrationTest extends BaseIntegrationSpec {

  "test" should {
    "edit with cyrillic text" in {

      val bot = getUkWikiBot
      val result = login(bot)
      result === "Success"

      val response = await(bot.page("User:IlyaBot/тест1").edit("текст", "коментар"))
      response === "Success"

    }
  }




}
