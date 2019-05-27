package org.scalawiki.wlx

import org.scalawiki.cache.CachedBot
import org.scalawiki.dto.Site
import org.scalawiki.wlx.dto.{Contest, Country}
import org.scalawiki.wlx.query.MonumentQuery
import org.specs2.mutable.Specification

class WlmUaListsSpec extends Specification {

  sequential

  val campaign = "wlm-ua"
  val cacheName = s"$campaign-2019"

  val contest = Contest.byCampaign(campaign).get.copy(year = 2019)
  val country = Country.Ukraine

  val bot = new CachedBot(Site.ukWiki, cacheName + "-wiki", true, entries = 100)
  val monumentQuery = MonumentQuery.create(contest)(bot)
  val monumentDb = MonumentDB.getMonumentDb(contest, monumentQuery)
  val all = monumentDb.allMonuments

  println(s"all size: ${all.size}")

  "places" should {
    "be mostly detected" in {
      all must not(beEmpty)
      val notFound = all.map { m =>
        s"${m.page}/${m.city.getOrElse("")}" -> country.byIdAndName(m.id, m.city.getOrElse(""))
      }.filter { case (m, cities) =>
        cities.isEmpty || cities.tail.nonEmpty
      }

      println(s"notFound size: ${notFound.size}")
      notFound.groupBy(_._1).mapValues { seq =>
        (seq.flatMap(_._2), seq.size)
      }.toSeq.sortBy { case (k, (v, s)) => -s }
        .map { case (k, (v, s)) =>
          val parents = v.map(_.parent().map(_.name).getOrElse("")).toSet.mkString(", ")
          s"$k - $s ($parents)"
        }
        .foreach(println)

      val percentage = notFound.size * 100 / all.size
      println(s"percentage: $percentage%")
      percentage should be < 5 // less than 5%
    }

    "not be just high level region" in {
      val oblasts = Country.Ukraine.regions.filter(adm => !Set("Київ", "Севастополь").contains(adm.name))
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
      percentage should be < 10 // less than 50%
    }
  }
}
