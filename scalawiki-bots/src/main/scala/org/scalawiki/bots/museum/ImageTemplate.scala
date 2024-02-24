package org.scalawiki.bots.museum

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._

object ImageTemplate {

  val parsed = ConfigFactory.parseResources("art-photo.conf")

  def makeInfoPage(
      title: String,
      description: String,
      location: String
  ): String = {
    val cfg = resolve(
      Map(
        "title" -> title,
        "description" -> description,
        "location" -> location
      )
    )

    cfg.getString("template")
  }

  def resolve(params: Map[String, AnyRef]): Config = {
    val paramsConfig = ConfigFactory.parseMap(params.asJava)
    parsed
      .withFallback(paramsConfig)
      .resolve()
  }
}
