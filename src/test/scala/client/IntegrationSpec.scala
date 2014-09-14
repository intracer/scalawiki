package client

import client.util.BaseIntegrationSpec

import scala.concurrent.Await

class IntegrationSpec extends BaseIntegrationSpec {

  "categoryMembers" should {
     "list files" in {
       val result = login(getUkWikiBot)
       result === "Success"

       val commons = getCommonsBot
       val images = Await.result(commons.page("Category:Images from Wiki Loves Earth 2014 in Ukraine").categoryMembers(), http.timeout)

       images.size must be_>(12000)

     }
  }



}
