package org.scalawiki.dto

import java.nio.file.{Files, Paths}
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter
import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Gallery
import org.scalawiki.wikitext.TemplateParser
import org.sweble.wikitext.engine.nodes.EngPage

case class ImageMetadata(data: Map[String, String]) {

  def camera: Option[String] = data.get("Model")

  def date: Option[ZonedDateTime] =
    data
      .get("DateTime")
      .map(s => LocalDateTime.parse(s, ImageMetadata.df).atZone(ZoneOffset.UTC))
}

object ImageMetadata {
  val pattern = "yyyy:MM:dd HH:mm:ss"
  val df = DateTimeFormatter.ofPattern(pattern)
}

case class Image(title: String,
                 url: Option[String] = None,
                 pageUrl: Option[String] = None,
                 size: Option[Long] = None,
                 width: Option[Int] = None,
                 height: Option[Int] = None,
                 author: Option[String] = None,
                 uploader: Option[User] = None,
                 year: Option[String] = None,
                 date: Option[ZonedDateTime] = None,
                 monumentIds: Seq[String] = Nil,
                 pageId: Option[Long] = None,
                 metadata: Option[ImageMetadata] = None,
                 categories: Set[String] = Set.empty,
                 specialNominations: Set[String] = Set.empty)
    extends Ordered[Image] {

  def compare(that: Image): Int = title.compareTo(that.title)

  def monumentId: Option[String] = monumentIds.headOption

  def download(filename: String): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    for (bytes <- MwBot.fromSite(Site.commons).getByteArray(url.get))
      Files.write(Paths.get(filename), bytes)
  }

  def pixels: Option[Long] = for (w <- width; h <- height) yield w * h

  def mpx: Option[Float] = pixels.map(_ / Math.pow(10, 6)).map(_.toFloat)

  def atLeastMpx(minMpxOpt: Option[Float]): Boolean =
    minMpxOpt.fold(true)(minMpx => mpx.exists(_ >= minMpx))

  def mpxStr: String = mpx.fold("")(v => f"$v%1.2f Mpx ")

  def resolution: Option[String] =
    for (w <- width; h <- height) yield w + " x " + h

  def resizeTo(resizeToX: Int, resizeToY: Int): Int =
    Image.resizedWidth(width.get, height.get, resizeToX, resizeToY)

  def withAuthor(newAuthor: String): Image = this.copy(author = Some(newAuthor))

  def withMonument(monumentId: String): Image =
    this.copy(monumentIds = Seq(monumentId))
}

object Image {

  val categoryRegex = "\\[\\[Category:([^]]+)\\]\\]".r

  def fromPageImages(page: Page): Option[Image] =
    page.images.headOption

  def fromPageRevision(
      page: Page,
      monumentIdTemplate: Option[String],
      specialNominationTemplates: Seq[String] = Nil): Option[Image] = {
    page.revisions.headOption.map { revision =>
      val content = revision.content.getOrElse("")
      val parsedPage = TemplateParser.parsePage(content)
      val ids = monumentIdTemplate.toList.flatMap { template =>
        TemplateParser
          .collectTemplates(parsedPage, template)
          .flatMap(_.getParamOpt("1"))
      }
      val specialNominations = specialNominationTemplates.flatMap { template =>
        Some(template).filter(_ => {
          val value = TemplateParser.collectTemplates(parsedPage, template)
          value.nonEmpty
        })
      }

      val author = getAuthorFromPage(parsedPage)

      // TODO category maps
      val categories = categoryRegex
        .findAllIn(content)
        .matchData
        .map(_.group(1).intern())
        .toSet

      new Image(
        page.title,
        author = Some(author),
        date = revision.timestamp,
        monumentIds = ids,
        pageId = page.id,
        categories = categories,
        specialNominations = specialNominations.toSet
      )
    }
  }

  def fromPage(page: Page,
               monumentIdTemplate: Option[String],
               specialNominationTemplates: Seq[String] = Nil): Option[Image] = {
    for (fromImage <- Image.fromPageImages(page);
         fromRev <- Image.fromPageRevision(page,
                                           monumentIdTemplate,
                                           specialNominationTemplates))
      yield {
        val renamedAuthor = fromRev.author.map(author =>
          AuthorsMap.renames.getOrElse(author, author))
        fromImage.copy(monumentIds = fromRev.monumentIds,
                       author = renamedAuthor,
                       categories = fromRev.categories,
                       specialNominations = fromRev.specialNominations)
      }
  }

  def getAuthorFromPage(content: String): String = {
    getAuthorFromPage(TemplateParser.parsePage(content))
  }

  def getAuthorFromPage(parsedPage: EngPage): String = {
    val template = TemplateParser.getTemplate(parsedPage, Some("Information"))
    val authorValue = template
      .flatMap { t =>
        t.getParamOpt("author").orElse(t.getParamOpt("Author"))
      }
      .getOrElse("")

    parseUser(authorValue)
  }

  def parseUser(authorValue: String): String = {
    val i1: Int = authorValue.indexOf("User:")
    val i2: Int = authorValue.indexOf("user:")
    val start = Seq(i1, i2, Int.MaxValue).filter(_ >= 0).min

    if (start < Int.MaxValue) {
      val pipe = authorValue.indexOf("|", start)
      val end =
        if (pipe >= 0)
          pipe
        else authorValue.length
      authorValue.substring(start + "user:".length, end)
    } else if (authorValue.contains('[')) {
      val extLinkStart = authorValue.indexOf('[')
      val wikiLinkStart = authorValue.indexOf("[[")
      val linkSpace = authorValue.indexOf(' ', extLinkStart)
      val extLinkEnd = authorValue.indexOf(']', linkSpace)
      if (extLinkStart != wikiLinkStart && linkSpace >= 0 && extLinkEnd >= 0) {
        authorValue.substring(linkSpace, extLinkEnd).trim
      } else {
        authorValue
      }
    } else {
      authorValue
    }
  }

  def basic(title: String,
            timestamp: Option[ZonedDateTime],
            uploader: Option[String],
            size: Option[Long],
            width: Option[Int],
            height: Option[Int],
            url: Option[String],
            pageUrl: Option[String],
            pageId: Option[Long],
            metadata: Option[Map[String, String]] = None) =
    new Image(
      title = title,
      date = timestamp,
      uploader = uploader.map(name => User(None, Some(name))),
      size = size,
      width = width,
      height = height,
      url = url,
      pageUrl = pageUrl,
      pageId = pageId,
      metadata = metadata.map(ImageMetadata.apply)
    )

  def gallery(images: Iterable[String],
              descriptions: Iterable[String] = Seq.empty): String =
    Gallery.asWiki(images, descriptions)

  def resizedWidth(w: Int, h: Int, resizeToX: Int, resizeToY: Int): Int = {
    val xRatio = w.toDouble / resizeToX
    val yRatio = h.toDouble / resizeToY

    val width = Math.min(resizeToX, w / yRatio)
    width.toInt
  }
}

object AuthorsMap {
  val renames = Map(
    "ЯдвигаВереск" -> "Wereskowa",
    "Михаил Титаренко Александрович" -> "Тітаренко Михайло"
  )
}
