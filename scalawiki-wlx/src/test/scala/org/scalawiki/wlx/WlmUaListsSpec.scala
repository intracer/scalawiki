package org.scalawiki.wlx

import org.scalawiki.cache.CachedBot
import org.scalawiki.dto.Site
import org.scalawiki.wlx.dto.{AdmDivision, Contest, Country, Monument}
import org.scalawiki.wlx.query.MonumentQuery
import org.specs2.mutable.Specification

case class UnknownPlace(page: String, regionId: String, name: String, candidates: Seq[AdmDivision], monuments: Seq[Monument]) {
  def parents: Set[String] = candidates.map(_.parent().map(_.name).getOrElse("")).toSet

  override def toString = {
    val candidatesStr = candidates.map { c =>
      c.parent().map(p => s"${p.name}(${p.code})/").getOrElse("") + s"${c.name}(${c.code})"
    }.mkString(", ")
    s"$page/$regionId/$name. monuments: ${monuments.size}" + (if (candidates.nonEmpty) s", Candidates: $candidatesStr" else "")
  }
}

class WlmUaListsSpec extends Specification {
  sys.props.put("jna.nosys", "true")

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

      val toFind = all.map(m => UnknownPlace(m.page,
        m.id.split("-").take(2).mkString("-"),
        m.city.getOrElse(""), Nil, Seq(m))
      ).groupBy(u => s"${u.page}/${u.regionId}/${u.name}")
        .mapValues { places =>
          places.head.copy(monuments = places.flatMap(_.monuments))
        }.values.toSeq

      val notFound = toFind.flatMap { p =>
        Some(p.copy(candidates = country.byIdAndName(p.regionId, p.name)))
          .filterNot(_.candidates.size == 1)
      }
      println(s"notFound size: ${notFound.size}")

      notFound
        .sortBy(-_.monuments.size)
        .foreach(println)

      val percentage = notFound.map(_.monuments.size).sum * 100 / all.size
      println(s"percentage: $percentage%")
      percentage should be < 5 // less than 1%
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
      percentage should be <= 5 // less than 10%
    }
  }
}
