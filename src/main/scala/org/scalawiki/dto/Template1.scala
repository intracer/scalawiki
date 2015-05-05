package org.scalawiki.dto

object Template1 {

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
        removeComments(s.substring(0, start) + s.substring(end + 3, s.length))
      }
      else s.substring(0, start)
    } else
      s
  }

}
