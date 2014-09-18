package client.dto

import java.util.regex.{Matcher, Pattern}

class Template(val text: String, val page: String = "") {

  def getParam(name: String): String = {
    findPosition(name).fold("") {
      case (start, end) =>
        text.substring(start, end).trim
    }
  }

  def getParamOpt(name: String): Option[String] = {
    findPosition(name)
      .fold[Option[String]](None) {
      case (start, end) =>
        val value = text.substring(start, end).trim
        if (value.isEmpty) None else Option(value)
    }
  }

  def hasTemplateParam(name: String): Boolean = matcher(text, name).find()

  def setTemplateParam(name: String, value: String): Template = {
    findPosition(name).fold(this) {
      case (start, end) =>
        init(text.substring(0, start) + value + text.substring(end), page)
    }
  }

  def matcher(text: String, param: String): Matcher = {
    val p = Pattern.compile("\\|\\s*" + param + "\\s*=")
    p.matcher(text)
  }

  def findPosition(param: String): Option[(Int, Int)] = {
    val m = matcher(text, param)
    if (m.find) {
      val start = m.end
      val newline = text.indexOf("\n", start)
      if (newline > 0)
        Some(start -> newline)
      else {
        val templateEnd = text.indexOf("}}", start)
        Some(start -> templateEnd)
      }
    } else None
  }

  def init(text: String, page:String ):Template = new Template(text, page)

}


object Template {

  def getDefaultParam(text: String, templateName: String) = {
    val template: String = "{{" + templateName + "|"

    val templateStart = text.indexOf(template)
    val end = text.indexOf("}}", templateStart)

    if (templateStart > 0 && end > 0) {
      text.substring(templateStart + template.length, end).trim.toLowerCase
    } else ""
  }

}
