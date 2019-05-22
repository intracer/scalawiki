package org.scalawiki.wlx

import org.scalawiki.cache.CachedBot
import org.scalawiki.dto.Site
import org.scalawiki.wlx.dto.{Contest, Country}
import org.scalawiki.wlx.query.MonumentQuery
import org.specs2.mutable.Specification

class WlmUaListsSpec extends Specification {

  val campaign = "wlm-ua"
  val contest = Contest.byCampaign(campaign).get.copy(year = 2019)
  val country = Country.Ukraine

  "lists" should {
    "be ok" in {
      contest.name === "Wiki Loves Monuments 2019 in Ukraine"
      val cacheName = s"$campaign-2019"
      val bot = new CachedBot(Site.ukWiki, cacheName + "-wiki", true, entries = 100)
      val monumentQuery = MonumentQuery.create(contest)(bot)
      val monumentDb = MonumentDB.getMonumentDb(contest, monumentQuery)

      val all = monumentDb.allMonuments
      all must not(beEmpty)
      println(s"all size: ${all.size}")
      val notFound = all.map { m =>
        val city = m.city.getOrElse("")
          .replace("[[", "")
          .replace("]]", "")
          .replace("село", "")
          .replace("смт", "")
          .replace("'''", "")
          .replace("''", "")
          .split("\\(").head
          .split("\\|").head
          .trim
        city -> country.byIdAndName(m.id, city)
      }.filter { case (m, cities) =>
        cities.isEmpty || cities.tail.nonEmpty
      }

      println(s"notFound size: ${notFound.size}")
      notFound.groupBy(_._1)
        .mapValues(_.size)
        .toSeq.sortBy { case (k, v) => -v }
        .map { case (k, v) => s"$k - $v " }
        .foreach(println)

      notFound.size * 100 / all.size should be < 20 // less than 20%
    }
  }
}
