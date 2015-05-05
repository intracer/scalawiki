package org.scalawiki.parser

import org.scalawiki.dto.Template2
import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.parser.nodes.{WtTemplate, WtTemplateArgument}

import scala.collection.JavaConverters._

object TemplateParser extends SwebleParser {

  val config: WikiConfig = DefaultConfigEnWp.generate

  def parseOne(wiki: String, templateName: Option[String] = None): Option[Template2] = {
    val page = parsePage("Some title", wiki).getPage
    findNode(page, { case t: WtTemplate if templateName.forall(getTemplateName(t).equals)  => nodeToTemplate(t) })
  }

  def parse(wiki: String, templateName: String): Seq[Template2] = {
    val page = parsePage("Some title", wiki).getPage
    collectNodes(page, { case t: WtTemplate if getTemplateName(t) == templateName => nodeToTemplate(t) })
  }

  def nodeToTemplate(template: WtTemplate): Template2 = {
    val args = template.getArgs.asScala.collect { case arg: WtTemplateArgument => arg }

    val params = args.zipWithIndex.map {
      case (arg, index) =>

        val name =
          if (arg.hasName)
            getText(arg.getName).trim
          else
            (index + 1).toString

        val value = getText(arg.getValue).trim

        name -> value
    }

    val name = getTemplateName(template)
    new Template2(
      name,
      params.toMap
    )
  }

  def getTemplateName(template: WtTemplate): String = getText(template.getName).trim

}
