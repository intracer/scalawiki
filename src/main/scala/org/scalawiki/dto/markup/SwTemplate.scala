package org.scalawiki.dto.markup

import org.sweble.wikitext.parser.nodes.{WtTemplate, WtTemplateArgument}

import scala.collection.JavaConverters._
import scala.collection.mutable

case class SwTemplate(wtNode: WtTemplate) extends SwNode {

  val name: String = getText(wtNode.getName).trim
  val args: mutable.Buffer[WtTemplateArgument] = wtNode.getArgs.asScala.collect { case arg: WtTemplateArgument => arg }
  val template = getTemplate

  def getTemplate = {
    val argsMap = args.zipWithIndex.map {
      case (arg, index) =>

        val name =
          if (arg.hasName)
            getText(arg.getName).trim
          else
            (index + 1).toString

        val value = getText(arg.getValue).trim

        name -> value
    }.toMap

    new Template(name, argsMap)
  }

  def getArg(name: String): Option[WtTemplateArgument] =
    args.find(p => p.getName.isResolved && p.getName.getAsString.trim == name)

  def setTemplateParam(name: String, value: String) = {
    getArg(name).foreach {
      arg =>
        val orig = getText(arg.getValue)

        val padded = if (orig.startsWith(" ") && !value.startsWith(" ")) " " + value else value
        val withNl = if (orig.endsWith("\n") && !value.endsWith("\n")) padded + "\n" else padded

        val f = NodeFactory
        val node = f.value(f.list(f.text(withNl)))
        arg.setValue(node)
    }
  }
}



