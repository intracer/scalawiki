package org.scalawiki.parser

import org.scalawiki.dto.markup.Table
import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.nodes.EngProcessedPage
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.engine.{PageId, PageTitle, WtEngineImpl}
import org.sweble.wikitext.parser.nodes._


trait TableParser {

  def parse(wiki: String): Table

}

// TODO is it thread safe?
object SwebleTableParser extends TableParser {

  import scala.collection.JavaConverters._

  val config: WikiConfig = DefaultConfigEnWp.generate

  def parse(wiki: String): Table = {

    val page = parse("Some title", wiki).getPage

    findNode(page, { case t: WtTableImplicitTableBody => t }).map {
      tableBody =>
        val rows = collectNodes(tableBody,  { case r: WtTableRow => r })

        val headers = rows.headOption.toSeq.flatMap(head => collectNodes(head, { case h: WtTableHeader => h }).map(getText))

        val items = rows.map {
          row => collectNodes(row, { case h: WtTableCell => h }).map(getText)
        }.filter(_.nonEmpty)

        new Table(headers, items, "", "")

    }.getOrElse(new Table(Seq.empty, Seq.empty, "", ""))

  }


  def parse(title: String, text: String): EngProcessedPage = {
    val engine: WtEngineImpl = new WtEngineImpl(config)
    val pageId = getPageId(title)
    engine.postprocess(pageId, text, null)
  }

  def getPageId(title: String): PageId = {
    val pageTitle: PageTitle = PageTitle.make(config, title)
    new PageId(pageTitle, -1)
  }

  def findNode[T](node: WtNode, pf: PartialFunction[WtNode, T]): Option[T] = {
    if (pf.isDefinedAt(node))
      Some(pf(node))
    else
      node.asScala.view.flatMap(child => findNode(child, pf)).headOption
  }

  def collectNodes[T](node: WtNode, pf: PartialFunction[WtNode, T]): Seq[T] = {
    if (pf.isDefinedAt(node))
      Seq(pf(node))
    else
      node.asScala.flatMap(child => collectNodes(child, pf))
  }

  def getText(node: WtNode): String =
    node match {
      case p: WtText => p.getContent.trim
      case p => p.asScala.map(getText).filter(_.trim.nonEmpty).mkString.trim
    }

}

