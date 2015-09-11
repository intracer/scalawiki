package org.scalawiki.wikitext

import org.scalawiki.dto.markup.{SwTemplate, Template}
import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.parser.nodes.WtTemplate

object TemplateParser extends SwebleParser {

  val config: WikiConfig = DefaultConfigEnWp.generate

  def parseOne(wiki: String, templateName: Option[String] = None): Option[Template] = {
    val page = parsePage("Some title", wiki).getPage
    findNode(page, { case t: WtTemplate if templateName.forall(getTemplateName(t).equals) => nodeToTemplate(t) })
  }

  def parse(wiki: String, templateName: String): Seq[Template] = {
    val page = parsePage("Some title", wiki).getPage
    collectNodes(page, { case t: WtTemplate if getTemplateName(t) == templateName => nodeToTemplate(t) })
  }

  def nodeToTemplate(wtTemplate: WtTemplate): Template = new SwTemplate(wtTemplate).getTemplate

}
