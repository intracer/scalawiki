package org.scalawiki.dto

import java.net.URLEncoder

case class Site(langCode: Option[String],
                family: String,
                domain: String,
                protocol: String = "https",
                port: Int = 80,
                scriptPath: String = "/w",
                script: String = "/w/index.php",
                articlePath: String = "/wiki") {

  val home = protocol + "://" + domain

  def pageUrl(title: String, urlEncode: Boolean = false) = {
    val underscored = title.replaceAll(" ", "_")
    home + articlePath + "/" + (
      if (urlEncode)
        URLEncoder.encode(underscored, "UTF-8")
      else
        underscored
      )
  }
}

object Site {

  def wikipedia(langCode: String) = project(langCode, "wikipedia")

  val commons = wikimedia("commons")

  val meta = wikimedia("meta")

  val enWiki = wikipedia("en")

  val ukWiki = wikipedia("uk")

  val localhost = {
    val scriptPath = "/mediawiki"
    val script = scriptPath + "/index.php"
    Site(None, "wikipedia", "localhost", "http", 8080, scriptPath, script, articlePath = script)
  }

  def project(langCode: String, family: String) =
    Site(Some(langCode), family, s"$langCode.$family.org")

  def wikimedia(code: String) = Site(None, code, s"$code.wikimedia.org")

  def host(host: String, port: Int = 80, protocol: String = "https"): Site = {
    val list = host.split("\\.").toList
    list match {
      case code :: "wikimedia" :: "org" :: Nil => wikimedia(code)
      case code :: family :: "org" :: Nil => project(code, family)
      case _ => Site(None, host, host, protocol, port)
    }
  }
}