package org.scalawiki.parser


import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.nodes.EngProcessedPage
import org.sweble.wikitext.engine.{PageId, PageTitle, WtEngineImpl}
import org.sweble.wikitext.parser.nodes._
import org.sweble.wikitext.parser.utils.WtRtDataPrinter

trait SwebleParser {

  import scala.collection.JavaConverters._

  def config: WikiConfig

  def parse(title: String, text: String): EngProcessedPage = {
    val engine = new WtEngineImpl(config)
    engine.postprocess(getPageId(title), text, null)
  }

  def getPageId(title: String): PageId = {
    new PageId(PageTitle.make(config, title), -1)
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

  def getText(node: WtNode): String = {
    WtRtDataPrinter.print(node)
  }

}
