package org.scalawiki.bots

import java.util.regex.Pattern

import org.scalawiki.dto.Page
import Pattern.quote

object PageFromFileBot {

  def pages(content: CharSequence,
            start: String = "{{-start-}}",
            end: String = "{{-end-}}",
            titleStart: String = "'''",
            titleEnd: String = "'''"): TraversableOnce[Page] = {
    val pageRegex = ("(?s)" + quote(start) + "\n(.*?)\n" + quote(end)).r
    val titleRegex = (quote(titleStart) + "(.*?)" + quote(titleEnd)).r

    pageRegex.findAllMatchIn(content).flatMap { m =>
      val text = m.group(1)
      val titleOption = titleRegex.findFirstMatchIn(text).map(_.group(1))

      titleOption.map(title => new Page(id = None, ns = 0, title = title).withText(text))
    }
  }

}
