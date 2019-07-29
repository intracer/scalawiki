package org.scalawiki.mockserver

import org.scalawiki.util.TestUtils.resourceAsString
import spray.util.pimpFuture

class ImagesMockServerSpec extends BaseMockServerSpec {

  val response = resourceAsString("/org/scalawiki/query/Commons_Wiki_Loves_Earth_2017_Winners.json")

  "images" should {
    "succesfully get images" in {
      val action = Map(
        "action" -> "query",
        "titles" -> "Commons:Wiki_Loves_Earth_2017/Winners",
        "prop" -> "imageinfo",
        "iiprop" -> "timestamp|user|size|url",
        "generator" -> "images",
        "gimlimit" -> "max"
      )

      stubOk(action, response)

      val bot = getBot

      val future = bot.page("Commons:Wiki_Loves_Earth_2017/Winners")
        .imageInfoByGenerator("images", "im",
          props = Set("timestamp", "user", "size", "url"))
      val info = future.await
      info should not(beEmpty)
      info.size === 382
      info.count(_.id.isEmpty) === 1
    }
  }


}
