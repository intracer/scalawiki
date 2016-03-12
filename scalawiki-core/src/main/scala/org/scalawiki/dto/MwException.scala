package org.scalawiki.dto

case class MwException(code: String, info: String, params: Map[String, String] = Map.empty)
  extends RuntimeException(
    s"MediaWiki Error: code: $code, info: $info, params: $params"
  )

//object MwException {
//  def noParams
//}
