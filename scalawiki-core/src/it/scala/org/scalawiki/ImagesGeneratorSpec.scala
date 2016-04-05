package org.scalawiki

import spray.util.pimpFuture

class ImagesGeneratorSpec extends BaseIntegrationSpec {

  "images" should {
    "succesfully get images" in {
      val bot = getCommonsBot
      val result = login(bot)
      result === "Success"

      val future = bot.page("User:Ilya/embeddedin").imageInfoByGenerator("images", "im", props = Set("timestamp", "user", "size", "url"), titlePrefix = Some(""))
      val info = future.await
      info should not (beEmpty)
    }
  }
}
