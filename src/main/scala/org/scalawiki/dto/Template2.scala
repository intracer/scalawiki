package org.scalawiki.dto

import scala.collection.mutable


case class Template2(templateName: String, params: mutable.LinkedHashMap[String, String] = mutable.LinkedHashMap.empty) {

}

