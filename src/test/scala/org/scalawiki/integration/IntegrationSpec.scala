package org.scalawiki.integration

import org.scalawiki.util.BaseIntegrationSpec

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

}
