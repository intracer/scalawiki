package org.scalawiki.bots.museum

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import net.ceedubs.ficus.FicusConfig

case class Diff[T](name: String, before: T, after: T)

case class EntryImage(filePath: String,
                      sourceDescription: Option[String] = None,
                      uploadTitle: Option[String] = None,
                      wikiDescription: Option[String] = None,
                      wlmId: Option[String] = None
                     ) {
  def diff(other: EntryImage): Seq[Diff[_]] = {
    (if (filePath != other.filePath)
      Seq(Diff("filePath", filePath, other.filePath))
    else Nil) ++
      (if (uploadTitle != other.uploadTitle)
        Seq(Diff("uploadTitle", uploadTitle, other.uploadTitle))
      else Nil) ++
      (if (sourceDescription != other.sourceDescription)
        Seq(Diff("sourceDescription", sourceDescription, other.sourceDescription))
      else Nil) ++
      (if (wikiDescription != other.wikiDescription)
        Seq(Diff("wikiDescription", wikiDescription, other.wikiDescription))
      else Nil) ++
      (if (wlmId != other.wlmId)
        Seq(Diff("wlmId", wlmId, other.wlmId))
      else Nil)
  }
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

  def descriptionLang(description: String) =
    s"{{$lang|$description}}"

  def interWikiLink(target: String) =
    s"[[:$lang:$target|]]"

  def imageOrParentWlmId(image: EntryImage) = image.wlmId.orElse(wlmId)

  def genWikiDescription(image: EntryImage): String =
    descriptionLang(image.sourceDescription.fold("")(_ + ", ") + interWikiLink(articleOrDir)) +
      imageOrParentWlmId(image).fold("")(id => s" {{Monument Ukraine|$id}}")

  def genUploadTitle(image: EntryImage, index: Int): String = s"$articleOrDir ${index + 1}"

  def withWikiDescriptions: Entry = {
    copy(images = images.map { img =>
      img.copy(wikiDescription = Some(genWikiDescription(img)))
    })
  }

  def withUploadTitles: Entry = {
    copy(images = images.zipWithIndex.map {
      case (img, index) =>
        img.copy(uploadTitle = Some(genUploadTitle(img, index)))
    })
  }

  def genImageFields = this.withUploadTitles.withWikiDescriptions

  def imagesMaps: Seq[Map[String, String]] = {
    images.map {
      image =>
        Map("file" -> image.filePath) ++
          image.uploadTitle.map("title" -> _) ++
          image.wikiDescription.map("description" -> _) ++
          image.wlmId.map("wlm-id" -> _) ++
          image.sourceDescription.map("source-description" -> _)
    }
  }

  def toConfig: Config = {
    import scala.collection.JavaConverters._

    val javaMaps = imagesMaps.map(_.asJava).asJava
    val imagesConf = ConfigValueFactory.fromIterable(javaMaps)

    val map = Map(
      "article" -> articleOrDir,
      "images" -> imagesConf
    ) ++ wlmId.map("wlm-id" -> _)

    ConfigFactory.parseMap(map.asJava)
  }

  def diff(other: Entry): Seq[Diff[_]] = {
    (if (dir != other.dir)
      Seq(Diff("dir", dir, other.dir))
    else Nil) ++
      (if (article != other.article)
        Seq(Diff("article", article, other.article))
      else Nil) ++
      (if (wlmId != other.wlmId)
        Seq(Diff("wlmId", wlmId, other.wlmId))
      else Nil) ++
      (if (images != other.images) {
        images.zip(other.images)
          .flatMap { case (img1, img2) => img1.diff(img2)
          }
      }
      else Nil)
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
      val description = imageCfg.getAs[String]("source-description")
      val uploadTitle = imageCfg.getAs[String]("title")
      val wikiDescription = imageCfg.getAs[String]("description")
      val imageWlmId = imageCfg.getAs[String]("wlm-id")
      EntryImage(path, description, uploadTitle, wikiDescription, imageWlmId)
    }

    Entry(dir,
      Some(article),
      wlmId,
      images,
      None
    )
  }
}
