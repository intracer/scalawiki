package org.scalawiki.wlx.dto

import com.typesafe.config.{Config, ConfigFactory}
import org.scalawiki.wlx.query.MonumentQuery
import net.ceedubs.ficus.Ficus._
import org.scalawiki.wlx.MonumentDB
import org.scalawiki.wlx.stat.ContestStat

import scala.util.Try

/**
 * Describes monument lists for contest special nominations
 *
 * @param name         Name of the special nomination
 * @param listTemplate name of template that monument lists consist of
 * @param pages        pages that contain lists of monuments, ot templates that contains links to these pages
 */
case class SpecialNomination(name: String, listTemplate: String, pages: Seq[String], years: Seq[Int] = Nil,
                             cities: Seq[String] = Nil)

object SpecialNomination {

  import scala.collection.JavaConverters._

  def load(name: String): Seq[SpecialNomination] = {
    fromConfig(ConfigFactory.load(name))
  }

  def fromConfig(config: Config): Seq[SpecialNomination] = {

    Try {
      config.getConfigList("nominations")
    }.recover { case x: Throwable =>
      println(x)
      java.util.List.of()
    }.toOption.map(_.asScala.toSeq.map { c =>
      new SpecialNomination(
        c.getString("name"),
        c.getString("listTemplate"),
        c.as[Option[Seq[String]]]("pages").getOrElse(Nil),
        c.as[Option[Seq[Int]]]("years").getOrElse(Nil),
        c.as[Option[Seq[String]]]("cities").getOrElse(Nil),
      )
    }).getOrElse(Seq.empty)
  }

  lazy val nominations = load("wlm_ua.conf")

  def getMonumentsMap(nominations: Seq[SpecialNomination], stat: ContestStat): Map[SpecialNomination, Seq[Monument]] = {
    val contest = stat.contest
    val monumentQuery = MonumentQuery.create(contest)
    nominations.map { nomination =>
      val monuments = if (nomination.pages.nonEmpty) {
        nomination.pages.flatMap { page =>
          monumentQuery.byPage(page, nomination.listTemplate)
        }
      } else {
        monumentsInCitites(nomination.cities, stat.monumentDb.get)
      }
      (nomination, monuments)
    }.toMap
  }

  def monumentsInCitites(cities: Seq[String], monumentDb: MonumentDB): Seq[Monument] = {
    val placeIds = placesByCities(cities).flatten.map(_.code).toSet
    monumentDb.allMonuments.filter { monument =>
      placeIds.contains(monumentDb.placeByMonumentId(monument.id))
    }
  }

  def placesByCities(cities: Seq[String]): Seq[Seq[AdmDivision]] = {
    cities.map { nameAndRegionCode =>
      val strings = nameAndRegionCode.split(" ")
      val name = strings.head
      val candidates = if (strings.size > 1) {
        Country.Ukraine.byIdAndName(strings.last, name)
      } else {
        Country.Ukraine.byName(name)
      }
      candidates.filterNot(_.regionType.exists(rt => Set("С", "Щ", "Т").contains(rt.code)))
    }
  }

}