package org.scalawiki.wlx

import org.scalawiki.cache.CachedBot
import org.scalawiki.dto.Site
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.query.MonumentQuery
import org.specs2.mutable.Specification

class WlmUaListsSpec extends Specification {
  sys.props.put("jna.nosys", "true")

  sequential

  val campaign = "wlm-ua"
  val cacheName = s"$campaign-2019"

  val contest = Contest.byCampaign(campaign).get.copy(year = 2019)
  val country = contest.country

  val bot = new CachedBot(Site.ukWiki, cacheName + "-wiki", true, entries = 1000)
  val monumentQuery = MonumentQuery.create(contest)(bot)
  val monumentDb = MonumentDB.getMonumentDb(contest, monumentQuery)
  val all = monumentDb.allMonuments

  "places" should {
    "be mostly detected" in {
      all must not(beEmpty)

      val notFound = monumentDb.unknownPlaces()

      val percentage = notFound.map(_.monuments.size).sum * 100 / all.size
      percentage should be < 5
    }

    "not be just high level region" in {
      val updater = new RegionFixerUpdater(monumentDb)
      updater.raions.size === 490

      val highLevel = all.filter(m => updater.raionNames.contains(m.cityName) && m.place.exists(_.trim.nonEmpty))

      val percentage = highLevel.size * 100 / all.size
      percentage should be <= 5
    }
  }
}