package org.scalawiki.wlx

import org.scalawiki.cache.CachedBot
import org.scalawiki.dto.Site
import org.scalawiki.wlx.dto.{AdmDivision, Contest, Country, Monument}
import org.scalawiki.wlx.query.MonumentQuery
import org.specs2.mutable.Specification

class WlmUaListsSpec extends Specification {
  sys.props.put("jna.nosys", "true")

  sequential

  val campaign = "wlm-ua"
  val cacheName = s"$campaign-2019"

  val contest = Contest.byCampaign(campaign).get.copy(year = 2019)
  val country = contest.country

  val bot = new CachedBot(Site.ukWiki, cacheName + "-wiki", true, entries = 100)
  val monumentQuery = MonumentQuery.create(contest)(bot)
  val monumentDb = MonumentDB.getMonumentDb(contest, monumentQuery)
  val all = monumentDb.allMonuments

  println(s"all size: ${all.size}")

  "places" should {
    "be mostly detected" in {
      all must not(beEmpty)

      val notFound = monumentDb.unknownPlaces()

      println(s"notFound size: ${notFound.size}")

      notFound
        .sortBy(-_.monuments.size)
        .foreach(println)

      val percentage = notFound.map(_.monuments.size).sum * 100 / all.size
      println(s"percentage: $percentage%")
      percentage should be < 5 // less than 1%
    }

    "not be just high level region" in {
      val oblasts = country.regions.filter(adm => !Set("Київ", "Севастополь").contains(adm.name))
      val raions = oblasts.flatMap(_.regions).filter(_.name.endsWith("район"))
      raions.size === 490
      val raionNames = raions.map(_.name).toSet

      val highLevel = all.filter(m => raionNames.contains(m.cityName) && m.place.exists(_.trim.nonEmpty))
      println(s"highLevel size: ${highLevel.size}")

      val canBeFixed = highLevel.filter { m =>
        m.place.exists { p =>
          country.byIdAndName(m.regionId, p.split(",").head).size == 1
        }
      }

      println(s"canBeFixed: ${canBeFixed.size}")

      highLevel.groupBy(_.page).toSeq.sortBy(-_._2.size).foreach { case (page, monuments) =>
        println(s"$page ${monuments.size} (${monuments.head.city.getOrElse("")})")
      }
      val percentage = highLevel.size * 100 / all.size
      println(s"percentage: $percentage%")
      percentage should be <= 5 // less than 10%
    }
  }
}
