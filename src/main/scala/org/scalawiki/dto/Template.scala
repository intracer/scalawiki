package org.scalawiki.dto

trait Template {

  def getParam(name: String, withoutComments: Boolean = true): String

  def getParamOpt(name: String): Option[String]

  def hasTemplateParam(name: String): Boolean

  def setTemplateParam(name: String, value: String): Template

  def text: String

  }
