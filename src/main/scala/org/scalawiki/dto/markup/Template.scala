package org.scalawiki.dto.markup

case class Template(
                     templateName: String,
                     params: Map[String, String] = Map.empty) {

  def hasTemplateParam(name: String): Boolean = params.contains(name)

  def getParam(name: String): String = params.getOrElse(name, "")

  def getParamOpt(name: String): Option[String] = params.get(name)

  def text: String = "{{" + templateName + params.map { case (k, v) => s"\n|$k = $v" }.mkString + "\n}}"

  def setTemplateParam(name: String, value: String): Template = {
    copy(params = params + (name -> value))
  }

}

