package org.scalawiki.parser

import org.scalawiki.dto.Template2
import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.parser.nodes.{WtTemplate, WtTemplateArgument}

import scala.collection.JavaConverters._
import scala.collection.mutable

object TemplateParser extends SwebleParser {

  val config: WikiConfig = DefaultConfigEnWp.generate

  def parse(wiki: String): Template2 = {

    val page = parse("Some title", wiki).getPage

    findNode(page, { case t: WtTemplate => t }).map {
      template =>

        val args = template.getArgs.asScala.collect { case arg: WtTemplateArgument => arg }

        val params = args.zipWithIndex.map {
          case (arg, index) =>
            val name = if (arg.hasName) getText(arg.getName).trim else (index + 1).toString
            val value = getText(arg.getValue).trim
            name -> value
        }

        new Template2(
          getText(template.getName).trim,
          mutable.LinkedHashMap(params:_*)
        )
    }.getOrElse(new Template2("", mutable.LinkedHashMap.empty))

  }
}
