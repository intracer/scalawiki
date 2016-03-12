package org.scalawiki

import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ImagesGeneratorSpec extends BaseIntegrationSpec {

  "images" should {
    "succesfully get images" in {
      val bot = getCommonsBot
      val result = login(bot)
      result === "Success"

      val future = bot.page("User:Ilya/embeddedin").imageInfoByGenerator("images", "im", props = Set("timestamp", "user", "size", "url"), titlePrefix = Some(""))
      val info = Await.result(future, Duration(5, TimeUnit.SECONDS))
      info should not (beEmpty)

    }
  }


}
