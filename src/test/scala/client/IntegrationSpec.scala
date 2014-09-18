package client

import client.util.BaseIntegrationSpec

class IntegrationSpec extends BaseIntegrationSpec {

  "categoryMembers" should {
     "list files" in {
       val result = login(getCommonsBot)
       result === "Success"

       val commons = getCommonsBot
       val images = await(commons.page("Category:Images from Wiki Loves Earth 2014 in Serbia").categoryMembers())

       images.size must be_>(500)

     }
  }

  "query meta" should {
    "get csrf token" in {

      val bot = getUkWikiBot
      val result = login(bot)
      result === "Success"

      val token = bot.token

      token must not be empty

    }
  }




}
