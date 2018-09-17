package org.scalawiki.wlx.dto

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Koatuu {

  implicit lazy val regionReads: Reads[Region] = (
    (__ \ "code").read[String] and
      (__ \ "name").read[String] and
      (__ \ "level2").lazyReadNullable(Reads.seq[Region](regionReads)).map(_.getOrElse(Nil))
    )(Region)

  def main(args: Array[String]): Unit = {

    val stream = getClass.getResourceAsStream("/koatuu.json")
    val json: JsValue = Json.parse(stream)

    val level1 = (json \ "level1").as[Seq[Region]]

    println(level1)
  }

}