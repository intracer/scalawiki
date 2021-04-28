package org.scalawiki.wlx.dto

import org.scalawiki.wlx.dto.Koatuu.{betterName, shortCode, skipGroups}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class AdmDivisionFlat(codeLevels: Seq[String], name: String, regionType: Option[RegionType]) {
  def code: String = codeLevels.last

  def toHierarchy(regions: Seq[AdmDivision],
                  parent: () => Option[AdmDivision]): AdmDivision = {
    AdmDivision(code, name, regions, parent, regionType)
  }

}

object AdmDivisionFlat {
  def apply(l1: String, l2: String, l3: String, l4: String, name: String, regionType: Option[RegionType]): AdmDivisionFlat = {
    AdmDivisionFlat(Seq(l1, l2, l3, l4).filterNot(_.isEmpty), name, regionType)
  }
}

object KoatuuNew {

  implicit val regionReads: Reads[AdmDivisionFlat] = (
    (__ \ "Перший рівень").read[String] and
      (__ \ "Другий рівень").read[String] and
      (__ \ "Третій рівень").read[String] and
      (__ \ "Четвертий рівень").read[String] and
      (__ \ "Назва об'єкта українською мовою").read[String].map(betterName) and
      (__ \ "Категорія").readNullable[String].map(_.flatMap(RegionTypes.codeToType.get))
    ) (AdmDivisionFlat.apply(_, _, _, _, _, _))

  def parse(json: JsValue): Seq[AdmDivisionFlat] = {
    json.as[Seq[AdmDivisionFlat]]
  }

  def makeHierarchy(flat: Seq[AdmDivisionFlat]): Seq[AdmDivision] = {
    flat.groupBy(_.codeLevels.head).map { case (code, regions) =>
      val (topList, subRegions) = regions.partition(_.codeLevels.size == 1)
      val top = topList.head
      top.toHierarchy(subRegions.map(_.toHierarchy(Nil, () => None)), () => Some(Country.Ukraine))
    }.toSeq
  }
}
