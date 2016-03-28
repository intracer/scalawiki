package org.scalawiki.bots.museum

import org.jsoup.Jsoup
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.safety.Whitelist

object HtmlParser {

  def replaceWs(s: String): String =
    s.replace('\u00a0', ' ')
      .replace('\u200b', ' ')
      .replace("&nbsp;", " ")

  def trimmedLines(html: String): Seq[String] = {
    val text = htmlText(html)
    text.split("\n")
      .map(replaceWs _ andThen (_.trim))
      .filter(_.nonEmpty)
      .toList
  }

  def htmlText(html: String): String = {
    val tags2Nl = Jsoup.clean(html, "",
      Whitelist.none().addTags("br", "p"),
      new OutputSettings().prettyPrint(true)
    )
    Jsoup.clean(tags2Nl, "",
      Whitelist.none(),
      new OutputSettings().prettyPrint(false)
    )
  }
}
