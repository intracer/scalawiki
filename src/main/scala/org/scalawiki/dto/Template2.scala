package org.scalawiki.dto

import scala.collection.mutable


case class Template2(
                      templateName: String,
                      params: mutable.LinkedHashMap[String, String] = mutable.LinkedHashMap.empty
                      ) extends Template {

  override def hasTemplateParam(name: String): Boolean = params.contains(name)

  override def getParam(name: String, withoutComments: Boolean): String = params.getOrElse(name, "")

  override def getParamOpt(name: String): Option[String] = params.get(name)

  override def text: String = "{{" + templateName + params.map{case (k, v) => s"\n| $k = $v"}.mkString + "\n}}"

  override def setTemplateParam(name: String, value: String): Template = {
    params += (name -> value)
    this
  }

}

