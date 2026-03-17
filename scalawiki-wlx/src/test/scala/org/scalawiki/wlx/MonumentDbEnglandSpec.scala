package org.scalawiki.wlx

import org.scalawiki.cache.CachedBot
import org.scalawiki.dto.Site
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.query.MonumentQuery
import org.specs2.mutable.Specification

class MonumentDbEnglandSpec extends Specification {

  "monument db" should {
    val contest = Contest.WLMEngland(2025)
    contest.listsHost === Some("en.wikipedia.org")
    val bot = new CachedBot(Site.wikipedia("en"), contest.campaign + "-wiki", true, entries = 1000)
    val monumentQuery = MonumentQuery.create(contest, bot, reportDifferentRegionIds = false)
    val db = MonumentDB.getMonumentDb(contest, monumentQuery)
    db.ids.size > 30000 should_=== true
  }

}
