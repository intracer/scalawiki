package org.scalawiki.wlx

import org.scalawiki.cache.CachedBot
import org.scalawiki.dto.Site
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.query.MonumentQuery
import org.specs2.mutable.Specification

class WlmUaListsSpec extends Specification {

  val campaign = "wlm-ua"
  val contest = Contest.byCampaign(campaign).get.copy(year = 2019)

  "lists" should {
    "be ok" in {
      contest.name === "Wiki Loves Monuments 2019 in Ukraine"
      val cacheName = s"$campaign-2019"
      val bot = new CachedBot(Site.ukWiki, cacheName + "-wiki", true, entries = 100)
      val monumentQuery = MonumentQuery.create(contest)(bot)
      val monumentDb = MonumentDB.getMonumentDb(contest, monumentQuery)

      monumentDb.allMonuments must not(beEmpty)

      ok
    }
  }

}
