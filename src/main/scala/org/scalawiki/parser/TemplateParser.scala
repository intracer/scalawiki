package org.scalawiki.parser

import org.scalawiki.dto.Template2
import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.parser.nodes.{WtTemplate, WtTemplateArgument}

import scala.collection.JavaConverters._
import scala.collection.mutable

object TemplateParser extends SwebleParser {

  val config: WikiConfig = DefaultConfigEnWp.generate

  def parseOne(wiki: String): Option[Template2] = {
    val page = parse("Some title", wiki).getPage
    findNode(page, { case t: WtTemplate => t }).map(nodeToTemplate)
  }

  def parse(wiki: String): Seq[Template2] = {
    val page = parse("Some title", wiki).getPage
    collectNodes(page, { case t: WtTemplate => t }).map(nodeToTemplate)
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

    new Template2(
      getText(template.getName).trim,
      mutable.LinkedHashMap(params: _*)
    )
  }
}
