package org.scalawiki.mockserver

import org.scalawiki.query.QueryLibrary
import org.scalawiki.util.TestUtils.resourceAsString
import spray.util.pimpFuture

class ImagesEmbeddedInMockServerSpec extends BaseMockServerSpec with QueryLibrary {

  val response = resourceAsString("/org/scalawiki/query/embeddedinIiPropRvProp.json")

  "images" should {
    "succesfully get images" in {
      val action = Map(
        "action" -> "query",
        "geititle" -> "Template:UkrainianNaturalHeritageSite",
        "prop" -> "info|revisions|imageinfo",
        "rvprop" -> "ids|content|timestamp|user|userid",
        "iiprop" -> "timestamp|user|size|metadata",
        "generator" -> "embeddedin",
        "geilimit" -> "500"
      )

      stubOk(action, response)

      val bot = getBot

      val future = bot.run(imagesByGenerator(generatorWithTemplate("UkrainianNaturalHeritageSite")))

      val info = future.await
      info should not(beEmpty)
      info.size === 50
    }
  }


}
