package org.scalawiki.wlx.dto

import com.typesafe.config.{Config, ConfigFactory}
import org.scalawiki.wlx.query.MonumentQuery

import scala.util.Try

/**
 * Describes monument lists for contest special nominations
 *
 * @param name         Name of the special nomination
 * @param listTemplate name of template that monument lists consist of
 * @param pages        pages that contain lists of monuments, ot templates that contains links to these pages
 */
case class SpecialNomination(name: String, listTemplate: String, pages: Seq[String], years: Seq[Int] = Nil)

object SpecialNomination {

  import scala.collection.JavaConverters._

  def load(name: String): Seq[SpecialNomination] = {
    fromConfig(ConfigFactory.load(name))
  }

  def fromConfig(config: Config): Seq[SpecialNomination] = {

    Try {
      config.getConfigList("nominations")
    }.toOption.map(_.asScala.map { c =>
        new SpecialNomination(
          c.getString("name"),
          c.getString("listTemplate"),
          c.getStringList("pages").asScala,
          if (c.hasPath("years")) c.getIntList("years").asScala.map(_.toInt) else Nil
        )
    }).getOrElse(Seq.empty)

  }

  lazy val nominations = load("wlm_ua.conf")

  def getMonumentsMap(nominations: Seq[SpecialNomination], monumentQuery: MonumentQuery): Map[SpecialNomination, Seq[Monument]] = {
    nominations.map { nomination =>
      val monuments = nomination.pages.flatMap { page =>
        monumentQuery.byPage(page, nomination.listTemplate)
      }
      (nomination, monuments)
    }.toMap
  }

}