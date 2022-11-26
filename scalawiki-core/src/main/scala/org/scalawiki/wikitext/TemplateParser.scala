package org.scalawiki.wikitext

import org.scalawiki.dto.markup.{SwTemplate, Template}
import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.nodes.EngPage
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.parser.nodes.WtTemplate

import scala.collection.mutable

object TemplateParser extends SwebleParser {

  val config: WikiConfig = DefaultConfigEnWp.generate

  def parsePage(wiki: String): EngPage = {
    parsePage("Some title", wiki).getPage
  }

  def getTemplate(page: EngPage, templateName: Option[String] = None): Option[Template] = {
    findNode(page, { case t: WtTemplate if templateName.forall(getTemplateName(t).equals) => nodeToTemplate(t) })
  }

  def collectTemplates(page: EngPage, templateName: String): mutable.Buffer[Template] = {
    collectNodes(page, { case t: WtTemplate if getTemplateName(t) == templateName => nodeToTemplate(t) })
  }

  def parseOne(wiki: String, templateName: Option[String] = None): Option[Template] = {
    getTemplate(parsePage(wiki), templateName)
  }

  def parse(wiki: String, templateName: String): mutable.Buffer[Template] = {
    val page = parsePage(wiki)
    collectTemplates(page, templateName)
  }

  def nodeToTemplate(wtTemplate: WtTemplate): Template = new SwTemplate(wtTemplate).getTemplate

}
