package org.scalawiki.mockserver

import org.scalawiki.util.TestUtils.resourceAsString
import spray.util.pimpFuture

class ImagesMockServerSpec extends BaseMockServerSpec {

  "images" should {
    "succesfully get image infos" in {
      val response = resourceAsString(
        "/org/scalawiki/query/Commons_Wiki_Loves_Earth_2017_Winners_imageInfos.json"
      )
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

      val future = bot
        .page("Commons:Wiki_Loves_Earth_2017/Winners")
        .imageInfoByGenerator(
          "images",
          "im",
          props = Set("timestamp", "user", "size", "url")
        )
      val info = future.await
      info should not(beEmpty)
      info.size === 382
      info.count(_.id.isEmpty) === 1
    }

    "succesfully get image revisions" in {
      val response = resourceAsString(
        "/org/scalawiki/query/Commons_Wiki_Loves_Earth_2019_Winners_revisions.json"
      )
      val action = Map(
        "action" -> "query",
        "titles" -> "Commons:Wiki_Loves_Earth_2019/Winners",
        "prop" -> "info|revisions",
        "rvprop" -> "ids|content|user|comment",
        "generator" -> "images",
        "gimlimit" -> "50"
      )

      stubOk(action, response)

      val bot = getBot

      val future = bot
        .page("Commons:Wiki_Loves_Earth_2019/Winners")
        .revisionsByGenerator(
          "images",
          "im",
          props = Set("ids", "content", "user", "comment"),
          limit = "50"
        )
      val info = future.await
      info should not(beEmpty)
      info.size === 50
      info.map(_.revisions.size) === List.fill(50)(1)
      info.map(_.revisions.head.content.exists(_.nonEmpty)) === List.fill(50)(
        true
      )
    }
  }
}
