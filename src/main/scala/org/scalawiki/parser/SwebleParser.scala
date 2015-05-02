package org.scalawiki.parser

import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.nodes.EngProcessedPage
import org.sweble.wikitext.engine.{PageId, PageTitle, WtEngineImpl}
import org.sweble.wikitext.parser.nodes._

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

  def getTextTrimmed(node: WtNode) = getText(node).trim

  def getText(node: WtNode): String =
    node match {

      case p: WtText =>
        p.getContent

      case p: WtInternalLink =>
        "[[" + getText(p.getTarget) + (if (p.hasTitle) "|" + getText(p.getTitle) else "") + "]]"

      case p: WtTemplate =>
        val args = p.getArgs.asScala.collect { case arg: WtTemplateArgument => arg }

        "{{" + getText(p.getName) +
          (if (args.nonEmpty)
            args.map {
              arg => "" + (if (arg.hasName) getText(arg.getName) + "=" else "") + getText(arg.getValue)
            }.mkString("|", "|", "}}")
          else "}}")

      case p: WtExternalLink =>
        "[" + p.getTarget.getProtocol + ":" + p.getTarget.getPath + " " + getText(p.getTitle) + "]"

      case p: WtTagExtension =>
        s"<${p.getName} ${getText(p.getXmlAttributes)}>${p.getBody.getContent}</${p.getName}>"

      case p: WtXmlAttribute =>
        getText(p.getName) + "=\"" + getText(p.getValue) + "\""

      case p =>
        p.asScala.map(getText).mkString
    }

}
