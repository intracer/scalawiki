package org.scalawiki.bots.museum

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import net.ceedubs.ficus.FicusConfig

case class EntryImage(filePath: String,
                      description: Option[String] = None,
                      wlmId: Option[String] = None,
                      lang: String = "uk") {

  def descriptionLang(description: String) =
    s"{{$lang|$description}}"

  def interWikiLink(target: String, title: String = "") =
    s"[[:$lang:$target|$title]]"

  def wikiDescription(article: String) =
    descriptionLang(description.fold("")(_ + ", ") + interWikiLink(article))
}

/**
  * Upload entry
  *
  * @param dir     directory
  * @param article wikipedia article
  * @param wlmId   Wiki Loves Monuments Id
  * @param images  images in the directory
  */
case class Entry(dir: String,
                 article: Option[String] = None,
                 wlmId: Option[String] = None,
                 images: Seq[EntryImage] = Seq.empty,
                 text: Option[String] = None,
                 lang: String = "uk") {

  val articleOrDir = article.getOrElse(dir)

  def imagesMaps: Seq[Map[String, String]] = {
    val maps = images.zipWithIndex.map {
      case (image, index) =>
        Map(
          "title" -> s"$articleOrDir ${index + 1}",
          "file" -> image.filePath,
          "description" -> image.wikiDescription(articleOrDir)
        ) ++
          image.wlmId.orElse(wlmId).map("wlm-id" -> _) ++
          image.description.map("source-description" -> _)
    }
    maps
  }

  def toConfig: Config = {
    import scala.collection.JavaConverters._

    val javaMaps = imagesMaps.map(_.asJava).asJava
    val imagesConf = ConfigValueFactory.fromIterable(javaMaps)
    ConfigFactory.parseMap(Map(
      "article" -> articleOrDir,
      "images" -> imagesConf
    ).asJava)
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
      Seq.empty, None)
  }

  def fromConfig(javaConfig: Config, dir: String) = {
    import net.ceedubs.ficus.Ficus._
    val cfg: FicusConfig = javaConfig

    val article = cfg.getOrElse[String]("article", dir)
    val wlmId = cfg.getAs[String]("wlm-id")
    val imagesCfg = cfg.as[Seq[FicusConfig]]("images")

    val images = imagesCfg.map { imageCfg =>
      val path = imageCfg.as[String]("file")
      val description = imageCfg.getAs[String]("description")
      EntryImage(path, description)
    }

    Entry(dir,
      Some(article),
      wlmId,
      images,
      None
    )
  }
}
