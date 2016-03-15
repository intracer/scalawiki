package org.scalawiki.bots.museum

import org.jsoup.Jsoup
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.safety.Whitelist

object HtmlParser {

  def replaceWs(s: String): String =
    s.replace('\u00a0', ' ').replace('\u200b', ' ').replace("&nbsp;", " ")

  def getLines(s: String): Seq[String] = {
    val text = Jsoup.clean(s, "", Whitelist.none(), new OutputSettings().prettyPrint(false))
    text.split("\n").map(replaceWs _ andThen(_.trim)).filter(_.nonEmpty).toList
  }

}
