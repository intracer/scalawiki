package org.scalawiki.dto

import java.net.URLEncoder

case class Site(
    langCode: Option[String],
    family: String,
    domain: String,
    protocol: String = "https",
    port: Option[Int] = None,
    scriptPath: String = "/w",
    script: String = "/w/index.php",
    articlePath: String = "/wiki"
) {

  val home: String = protocol + "://" + domain

  def pageUrl(title: String, urlEncode: Boolean = false): String = {
    val underscored = title.replaceAll(" ", "_")
    home + articlePath + "/" + (
      if (urlEncode)
        URLEncoder.encode(underscored, "UTF-8")
      else
        underscored
    )
  }

  def portStr: String = port.fold("")(":" + _)
}

object Site {

  def wikipedia(langCode: String): Site = project(langCode, "wikipedia")

  val commons: Site = wikimedia("commons")

  val meta: Site = wikimedia("meta")

  val enWiki: Site = wikipedia("en")

  val ukWiki: Site = wikipedia("uk")

  def localhost: Site = {
    val scriptPath = "/w"
    val script = scriptPath + "/index.php"
    Site(
      None,
      "wikipedia",
      "localhost",
      "http",
      Some(8080),
      scriptPath,
      script,
      articlePath = script
    )
  }

  def project(langCode: String, family: String): Site =
    Site(Some(langCode), family, s"$langCode.$family.org")

  def wikimedia(code: String): Site = Site(None, code, s"$code.wikimedia.org")

  def host(
      host: String,
      protocol: String = "https",
      port: Option[Int] = None
  ): Site = {
    val list = host.split("\\.").toList
    list match {
      case code :: "wikimedia" :: "org" :: Nil => wikimedia(code)
      case code :: family :: "org" :: Nil      => project(code, family)
      case _                                   => Site(None, host, host, protocol, port)
    }
  }
}
