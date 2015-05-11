package org.scalawiki.wlx.dto

import java.nio.file.{Files, Paths}

import org.scalawiki.MwBot
import org.scalawiki.dto.{User, Page}
import org.scalawiki.parser.TemplateParser


case class Image(title: String,
                 url: Option[String] = None,
                 pageUrl: Option[String] = None,
                 size: Option[Long] = None,
                 width: Option[Int] = None,
                 height: Option[Int] = None,
                 author: Option[String] = None,
                 uploader: Option[User] = None,
                 year: Option[String] = None,
                 date: Option[String] = None,
                 monumentId: Option[String] = None,
                 pageId: Option[Long] = None
                  ) extends Ordered[Image] {

  def compare(that: Image) = title.compareTo(that.title)

  //  def region: Option[String] = monumentId.map(_.split("-")(0))

  def download(filename: String) {
    import scala.concurrent.ExecutionContext.Implicits.global
    for (bytes <- MwBot.get(MwBot.commons).getByteArray(url.get))
      Files.write(Paths.get(filename), bytes)
  }

  def pixels = width.get * height.get

}

object Image {

  def fromPageImages(page: Page, monumentIdTemplate: String, year: String): Option[Image] =
    page.images.headOption.map(_.copy(year = Option(year)))

  def fromPageRevision(page: Page, monumentIdTemplate: String, date: String): Option[Image] = {
    page.revisions.headOption.map { revision =>

      val idRegex = """(\d\d)-(\d\d\d)-(\d\d\d\d)"""
      val content = revision.content.getOrElse("")
      val idOpt = TemplateParser.parseOne(content, Some(monumentIdTemplate)).flatMap(_.getParamOpt("1"))
      //val ipOpt = if (id.matches(idRegex)) Some(id) else None

      val author = getAuthorFromPage(content)

      new Image(page.title,
        author = Some(author),
        date = Some(date),
        monumentId = idOpt,
        pageId = page.id)
    }
  }

  def getAuthorFromPage(content: String): String = {
    val template = TemplateParser.parseOne(content, Some("Information"))
    val authorValue = template.flatMap(t => t.getParamOpt("author").orElse(t.getParamOpt("Author"))).getOrElse("")

    val i1: Int = authorValue.indexOf("User:")
    val i2: Int = authorValue.indexOf("user:")
    val start = Seq(i1, i2, Int.MaxValue).filter(_ >= 0).min

    if (start < Int.MaxValue) {
      val pipe = authorValue.indexOf("|", start)
      val end = if (pipe >= 0)
        pipe
      else authorValue.length
      authorValue.substring(start + "user:".length, end)
    }
    else
      authorValue
  }

  def basic(timestamp: String,
            uploader: String,
            size: Long,
            width: Int,
            height: Int,
            url: String,
            pageUrl: String)
  = new Image(
    title = "",
    date = Option(timestamp),
    uploader = Some(User(None, Some(uploader))),
    size = Some(size),
    width = Some(width),
    height = Some(height),
    url = Option(url),
    pageUrl = Option(pageUrl))
}
