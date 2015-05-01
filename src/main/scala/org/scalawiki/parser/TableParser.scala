package org.scalawiki.parser

import org.scalawiki.dto.markup.Table
import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.parser.nodes._


trait TableParser {

  def parse(wiki: String): Table

}

// TODO is it thread safe?
object SwebleTableParser extends TableParser with SwebleParser {

  val config: WikiConfig = DefaultConfigEnWp.generate

  def parse(wiki: String): Table = {

    val page = parse("Some title", wiki).getPage

    findNode(page, { case t: WtTableImplicitTableBody => t }).map {
      tableBody =>
        val rows = collectNodes(tableBody,  { case r: WtTableRow => r })

        val headers = rows.headOption.toSeq.flatMap(head => collectNodes(head, { case h: WtTableHeader => h }).map(h => getText(h).trim))

        val items = rows.map {
          row => collectNodes(row, { case c: WtTableCell => c }).map(c => getText(c).trim)
        }.filter(_.nonEmpty)

        new Table(headers, items, "", "")

    }.getOrElse(new Table(Seq.empty, Seq.empty, "", ""))

  }

}

