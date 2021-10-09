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
                             cities: Seq[AdmDivision] = Nil)

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
      new java.util.ArrayList()
    }.toOption.map(_.asScala.toSeq.map { c =>
      new SpecialNomination(
        c.getString("name"),
        c.getString("listTemplate"),
        c.as[Option[Seq[String]]]("pages").getOrElse(Nil),
        c.as[Option[Seq[Int]]]("years").getOrElse(Nil),
        if (c.hasPath("cities")) {
          c.getConfigList("cities").asScala.toSeq.map { citiConf =>
            val name = citiConf.getString("name")
            val code = citiConf.getString("code")
            lookupCity(name, code).head
          }
        } else Nil
      )
    }).getOrElse(Seq.empty)
  }

  lazy val nominations = load("wlm_ua.conf")

  def getMonumentsMap(nominations: Seq[SpecialNomination], stat: ContestStat): Map[SpecialNomination, Seq[Monument]] = {
    val contest = stat.contest
    val monumentQuery = MonumentQuery.create(contest, reportDifferentRegionIds = false)
    nominations.map { nomination =>
      val monuments = if (nomination.pages.nonEmpty) {
        nomination.pages.flatMap { page =>
          monumentQuery.byPage(page, nomination.listTemplate)
        }
      } else {
        monumentsInCities(nomination.cities, stat.monumentDb.get)
      }
      (nomination, monuments)
    }.toMap
  }

  def monumentsInCities(cities: Seq[AdmDivision], monumentDb: MonumentDB): Seq[Monument] = {
    val placeIds = cities.map(_.code).toSet
    monumentDb.allMonuments.filter { monument =>
      monumentDb.placeByMonumentId.get(monument.id).exists(placeIds.contains)
    }
  }

  def lookupCity(name: String, code: String): Seq[AdmDivision] = {
    Country.Ukraine.byIdAndName(code.take(2), name).filter(_.code == code)
  }

}