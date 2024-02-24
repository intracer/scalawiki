package org.scalawiki.bots

import java.util.regex.Pattern.quote
import org.scalawiki.dto.Page
import scala.collection.TraversableOnce

case class PageFromFileFormat(
    start: String = "{{-start-}}",
    end: String = "{{-end-}}",
    titleStart: String = "'''",
    titleEnd: String = "'''"
)

object PageFromFileBot {

  def pages(
      content: CharSequence,
      fmt: PageFromFileFormat = PageFromFileFormat(),
      noTitle: Boolean = false
  ): TraversableOnce[Page] = {
    val pageRegex =
      ("(?s)" + quote(fmt.start) + "\r?\n(.*?)\r?\n" + quote(fmt.end)).r
    val titleRegex = (quote(fmt.titleStart) + "(.*?)" + quote(fmt.titleEnd)).r

    pageRegex.findAllMatchIn(content).flatMap { m =>
      val text = m.group(1)
      val titleOption = titleRegex.findFirstMatchIn(text).map(_.group(1))

      titleOption.map { title =>
        val pageText =
          if (noTitle)
            titleRegex.replaceFirstIn(text, "")
          else text

        Page(title).withText(pageText)
      }
    }
  }

  def join(
      pages: Seq[Page],
      fmt: PageFromFileFormat = PageFromFileFormat(),
      includeTitle: Boolean = true
  ): String = {
    pages
      .map { page =>
        val title =
          if (includeTitle)
            fmt.titleStart + page.title + fmt.titleEnd
          else ""

        fmt.start + "\n" +
          title +
          page.text.getOrElse("") +
          "\n" + fmt.end
      }
      .mkString("\n")
  }
}
