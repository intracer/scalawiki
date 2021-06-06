package org.scalawiki.wlx.dto

import org.scalawiki.wlx.dto.Koatuu.betterName
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

object KoatuuNew {

  val readStringFromLong: Reads[String] = implicitly[Reads[Long]].map(x => x.toString)

  def stringOrLong(name: String): Reads[String] = {
    (__ \ name).read[String] or
      (__ \ name).read[String](readStringFromLong)
  }

  implicit val regionReads: Reads[AdmDivisionFlat] = (
    stringOrLong("Перший рівень") and
      stringOrLong("Другий рівень") and
      stringOrLong("Третій рівень") and
      stringOrLong("Четвертий рівень") and
      (__ \ "Назва об'єкта українською мовою").read[String].map(betterName) and
      (__ \ "Категорія").readNullable[String].map(_.flatMap(KoatuuTypes.codeToType.get))
    ) (AdmDivisionFlat.apply(_, _, _, _, _, _))

  def parse(json: JsValue): Seq[AdmDivisionFlat] = {
    json.as[Seq[AdmDivisionFlat]]
  }

}
