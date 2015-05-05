package org.scalawiki.wlx.dto

import java.nio.file.{Files, Paths}

import org.scalawiki.MwBot
import org.scalawiki.dto.{Page, Template1}
import org.scalawiki.parser.TemplateParser


case class Image(pageId: Long, title: String,
                 url: String, pageUrl: String,
                 size: Long,
                 width: Int,
                 height: Int,
                 monumentId: Option[String] = None,
                 author: Option[String] = None,
                 uploader: Option[String] = None,
                 year: Option[String] = None,
                 date: Option[String] = None) extends Ordered[Image] {

  def compare(that: Image) = (this.pageId - that.pageId).signum

  //  def region: Option[String] = monumentId.map(_.split("-")(0))

  def download(filename: String) {
    import scala.concurrent.ExecutionContext.Implicits.global
    for (bytes <- MwBot.get(MwBot.commons).getByteArray(url))
      Files.write(Paths.get(filename), bytes)
  }

}

object Image {

  def fromPageImageInfo(page: Page, monumentIdTemplate: String, year: String): Option[Image] = {
    page.imageInfo.headOption.map { ii =>
      Image(
        pageId = page.id.get,
        title = page.title,
        url = ii.url,
        pageUrl = ii.descriptionUrl,
        size = ii.size,
        width = ii.width,
        height = ii.height,
        monumentId = None,
        author = None,
        Some(ii.uploader),
        Some(year),
        Some(ii.timestamp)
      )
    }
  }

  def fromPageRevision(page: Page, monumentIdTemplate: String, date: String): Option[Image] = {
    page.revisions.headOption.map { revision =>

      val idRegex = """(\d\d)-(\d\d\d)-(\d\d\d\d)"""
      val content = revision.content.getOrElse("")
      val id = Template1.getDefaultParam(content, monumentIdTemplate)
      val ipOpt = if (id.matches(idRegex)) Some(id) else None

      val author = getAuthorFromPage(content)

      //      val author = authorValue.split("\\|")(0).replace("[[User:", "").replace("[[user:", "")

      new Image(page.id.get, page.title, "", "", 0, 0, 0, ipOpt, Some(author), None, Some(date))
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
}
