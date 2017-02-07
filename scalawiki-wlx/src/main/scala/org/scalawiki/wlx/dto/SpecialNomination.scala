package org.scalawiki.wlx.dto

import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Try

/**
  * Describes monument lists for contest special nominations
  *
  * @param name         Name of the special nomination
  * @param listTemplate name of template that monument lists consist of
  * @param pages        pages that contain lists of monuments, ot templates that contains links to these pages
  */
case class SpecialNomination(name: String, listTemplate: String, pages: Seq[String])

object SpecialNomination {

  import scala.collection.JavaConverters._

  def load(name: String): Seq[SpecialNomination] = {
    fromConfig(ConfigFactory.load(name))
  }

  def fromConfig(config: Config): Seq[SpecialNomination] = {

    Try {
      config.getConfigList("nominations")
    }.toOption.map(_.asScala.map {
      c =>
        val (name, listTemplate, pages) = (
          c.getString("name"),
          c.getString("listTemplate"),
          c.getStringList("pages"))

        new SpecialNomination(name, listTemplate, pages.asScala)
    }).getOrElse(Seq.empty)

  }

  lazy val nominations = load("wlm_ua.conf")
}