package org.scalawiki.bots.museum

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

/**
  * Upload entry
  *
  * @param dir          directory
  * @param article      wikipedia article
  * @param wlmId        Wiki Loves Monuments Id
  * @param images       images in the directory
  * @param descriptions image descriptions
  */
case class Entry(dir: String,
                 article: Option[String] = None,
                 wlmId: Option[String] = None,
                 images: Seq[String] = Seq.empty,
                 descriptions: Seq[String] = Seq.empty,
                 text: Option[String] = None,
                 lang: String = "uk") {

  def descriptionLang(description: String) =
    s"{{$lang|$description}}"

  def interWikiLink(target: String, title: String = "") =
    s"[[:$lang:$target|$title]]"

  def imagesMaps: Seq[Map[String, String]] = {
    val maps = images.zip(descriptions).zipWithIndex.map {
      case ((image, description), index) =>
        val articleOrDir = article.getOrElse(dir)
        val wikiDescription = descriptionLang(description + ", " + interWikiLink(articleOrDir))
        Map(
          "title" -> s"$articleOrDir ${index + 1}",
          "file" -> image,
          "description" -> wikiDescription
        ) ++ wlmId.map("wlm-id" -> _)
    }
    maps
  }

  def toConfig: Config = {
    import scala.collection.JavaConverters._

    val javaMaps = imagesMaps.map(_.asJava).asJava
    val imagesConf = ConfigValueFactory.fromIterable(javaMaps)
    ConfigFactory.empty()
      .withValue("images", imagesConf)
  }
}

object Entry {
  def fromRow(row: Iterable[String]) = {
    val seq = row.toSeq

    val (dir, article, wlmId) = seq match {
      case Seq(dir, article, wlmId, _*) => (dir, Some(article), Some(wlmId))
      case Seq(dir, article) => (dir, Some(article), None)
      case Seq(dir) => (dir, None, None)
    }

    Entry(dir,
      article.filter(_.trim.nonEmpty),
      wlmId.filter(_.trim.nonEmpty),
      Seq.empty, Seq.empty, None)
  }
}
