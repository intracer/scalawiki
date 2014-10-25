package client.dto

import java.util.regex.{Matcher, Pattern}

class Template(val text: String, val containingPage: String = "") {

  def getParam(name: String, withoutComments: Boolean = true): String = {
    val param = findPosition(name).fold("") {
      case (start, end) =>
        text.substring(start, end)
    }
    if (withoutComments)
      Template.removeComments(param).trim
    else param.trim
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
        init(text.substring(0, start) + value + text.substring(end), containingPage)
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

      val linkStart = text.indexOf("[[", start)
      var nextPipe = text.indexOf("\n|", start)
      if  (nextPipe < 0)
        nextPipe = text.indexOf("|", start)
      var linkEnd = -1

      if (linkStart > 0) {
        linkEnd = text.indexOf("]]", linkStart + 2)
      }

      if (nextPipe > linkStart && nextPipe < linkEnd) {
        var nextPipe = text.indexOf("\n|", linkEnd)
        if  (nextPipe < 0)
          nextPipe = text.indexOf("|", linkEnd)
      }

      //      val newline = text.indexOf("\n", start)
      val templateEnd = text.indexOf("}}", start)
      val stringEnd = text.size - 1

      val end = Seq(nextPipe, templateEnd, stringEnd).filter(_ >= 0).min
      Some(start -> end)
    } else None
  }

  def init(text: String, page: String): Template = new Template(text, page)

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

  def removeComments(s: String): String = {
    val start = s.indexOf("<!--")

    if (start > 0) {
      val end = s.indexOf("-->", start + 4)
      if (end > 0) {
        removeComments(s.substring(0, start) + s.substring(end + 3, s.size))
      }
      else s.substring(0, start)
    } else
      s
  }

}
