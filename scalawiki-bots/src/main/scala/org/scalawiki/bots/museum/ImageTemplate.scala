package org.scalawiki.bots.museum

import com.typesafe.config.{ConfigResolveOptions, ConfigFactory}

import scala.collection.JavaConverters._


object ImageTemplate {

  def resolve(params: Map[String, AnyRef]) = {
    val parsed = ConfigFactory.parseResources("art-photo.conf")
    val paramsConfig = ConfigFactory.parseMap(params.asJava)
    parsed
      .withFallback(paramsConfig)
      .resolve()

    //ConfigFactory.load("art-photo.conf")
  }
}
