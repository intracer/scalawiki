package org.scalawiki.wlx.dto

import play.api.libs.json._

object Koatuu {

  def main(args: Array[String]): Unit = {

    implicit val regionReads = Json.reads[Region]

    val stream = getClass.getResourceAsStream("/koatuu.json")
    val json: JsValue = Json.parse(stream)

    val level1 = (json \ "level1").as[Seq[Region]]

    println(level1)
  }

}