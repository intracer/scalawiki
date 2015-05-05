package org.scalawiki.dto


case class Template2(
                      templateName: String,
                      params: Map[String, String] = Map.empty
                      ) extends Template {

  override def hasTemplateParam(name: String): Boolean = params.contains(name)

  override def getParam(name: String, withoutComments: Boolean): String = params.getOrElse(name, "")

  override def getParamOpt(name: String): Option[String] = params.get(name)

  override def text: String = "{{" + templateName + params.map{case (k, v) => s"\n|$k = $v"}.mkString + "\n}}"

  override def setTemplateParam(name: String, value: String): Template = {
    copy(params = params + (name -> value))
  }

}

