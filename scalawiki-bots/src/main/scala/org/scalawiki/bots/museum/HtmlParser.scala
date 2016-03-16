package org.scalawiki.bots.museum

import org.jsoup.Jsoup
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.safety.Whitelist

object HtmlParser {

  def replaceWs(s: String): String =
    s.replace('\u00a0', ' ').replace('\u200b', ' ').replace("&nbsp;", " ")

  def trimmedLines(s: String): Seq[String] = {
    val tags2Nl = Jsoup.clean(s, "", Whitelist.none().addTags("br", "p"), new OutputSettings().prettyPrint(true))
    val text = Jsoup.clean(tags2Nl, "", Whitelist.none(), new OutputSettings().prettyPrint(false))
    text.split("\n").map(replaceWs _ andThen(_.trim)).filter(_.nonEmpty).toList
  }

}
