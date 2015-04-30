package org.scalawiki.parser

import org.scalawiki.dto.markup.Table

object TableParser {

  def parse(wiki: String): Table = {

    val start = wiki.indexOf("{|")
    val end = wiki.indexOf("|}")
    val body = wiki.substring(start, end)

    val rowsLines = body.split("\\|\\-").map(_.split("\n"))

    val headersLines = rowsLines.map(_.filter(_.startsWith("!"))).filter(_.nonEmpty)
    val dataLines = rowsLines.map(_.filter(_.startsWith("|"))).filter(_.nonEmpty)

    val headers = headersLines.headOption.toSeq.flatMap(_.head.substring(1).split("\\!\\!").map(_.trim).toSeq)
    val data = dataLines.map(_.head.substring(1).split("\\|\\|").map(_.trim).toSeq)

    new Table("", headers, data, "")
  }

}
