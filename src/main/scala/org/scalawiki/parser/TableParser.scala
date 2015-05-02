package org.scalawiki.parser

import org.scalawiki.dto.markup.Table
import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.parser.nodes._

object TableParser extends SwebleParser {

  val config: WikiConfig = DefaultConfigEnWp.generate

  def parse(wiki: String): Table = {

    val page = parse("Some title", wiki).getPage

    findNode(page, { case t: WtTableImplicitTableBody => t }).map {
      tableBody =>
        val rows = collectNodes(tableBody,  { case r: WtTableRow => r })

        val headers = rows.headOption.toSeq.flatMap {
          head =>
            collectNodes(head, { case h: WtTableHeader => h }).map(getTextTrimmed)
        }

        val items = rows.map {
          row => collectNodes(row, { case c: WtTableCell => c }).map(getTextTrimmed)
        }.filter(_.nonEmpty)

        new Table(headers, items, "", "")

    }.getOrElse(new Table(Seq.empty, Seq.empty, "", ""))

  }

}

