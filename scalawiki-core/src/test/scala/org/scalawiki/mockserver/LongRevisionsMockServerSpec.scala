package org.scalawiki.mockserver

import org.scalawiki.query.QueryLibrary
import org.scalawiki.util.TestUtils.resourceAsString
import spray.util.pimpFuture

import scala.concurrent.ExecutionContext.Implicits.global

class LongRevisionsMockServerSpec extends BaseMockServerSpec with QueryLibrary {

  val response = resourceAsString(
    "/org/scalawiki/query/longRevisionHistory.json"
  )

  "revisions" should {
    "succesfully get long response" in {
      val action = Map(
        "action" -> "query",
        "pageids" -> "3126080",
        "prop" -> "info|revisions",
        "rvprop" -> "content|ids|size|user|userid|timestamp",
        "rvlimit" -> "max"
      )

      stubOk(action, response)

      val bot = getBot

      val future = bot.run(pageRevisionsQuery(3126080)).map(_.headOption)
      val pageOption = future.await

      pageOption should not(beNone)
      val page = pageOption.get
      page.id === Some(3126080)
      page.title === "Список птахів Швеції"
      page.revisions.size === 50
    }
  }
}
