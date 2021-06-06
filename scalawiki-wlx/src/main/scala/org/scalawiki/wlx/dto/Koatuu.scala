package org.scalawiki.wlx.dto

import org.scalawiki.wlx.dto.KoatuuNew.parse
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class RegionType(code: String, names: Seq[String],
                      nameSuffix: Option[String] = None,
                      noSuffix: Set[String] = Set.empty)

trait RegionTypes {

  def regionTypes: Seq[RegionType]

  def codeToType: Map[String, RegionType]

  def groupTypes(types: Seq[RegionType]): Map[String, RegionType] = types.groupBy(_.code).mapValues(_.head).toMap

  def nameToType(name: String): Seq[RegionType] = regionTypes.filter(_.names.exists(name.toLowerCase.contains))

}

object KoatuuTypes extends RegionTypes {

  override val regionTypes = Seq(
    RegionType("Р", Seq("район")),
    RegionType("Т", Seq("селище міського типу", "смт")),
    RegionType("С", Seq("село", "c.")),
    RegionType("Щ", Seq("селище", "с-ще")),
    RegionType("М", Seq("місто", "м.")),
  )

  override val codeToType = groupTypes(regionTypes)

}

object Koatuu {

  def regions(parent: () => Option[AdmDivision] = () => None): Seq[AdmDivision] = {
    val stream = getClass.getResourceAsStream("/koatuu.json")
    val json = Json.parse(stream)

    implicit val level2Reads: Reads[AdmDivision] = regionReads(2, parent)
    (json \ "level1").as[Seq[AdmDivision]].map(_.withParents(parent))
  }

  def regionsNew(parent: () => Option[AdmDivision] = () => None, size: Int = 1): Seq[AdmDivision] = {
    val stream = getClass.getResourceAsStream("/koatuu_new.json")
    val json = Json.parse(stream)
    AdmDivisionFlat.makeHierarchy(parse(json), parent)
  }

  def regionReads(level: Int, parent: () => Option[AdmDivision] = () => None): Reads[AdmDivision] = (
    (__ \ "code").read[String].map(c => shortCode(c, level)) and
      (__ \ "name").read[String].map(betterName) and
      (__ \ ("level" + level))
        .lazyReadNullable(Reads.seq[AdmDivision](regionReads(level + 1)))
        .map(_.getOrElse(Nil)).map(skipGroups) and
      Reads.pure(parent) and
      (__ \ "type").readNullable[String].map(_.flatMap(KoatuuTypes.codeToType.get))
    ) (AdmDivision.apply(_, _, _, _, _))

  val groupNames = Seq("Міста обласного підпорядкування", "Міста", "Райони", "Селища міського типу", "Населені пункти", "Сільради")
    .map(_.toUpperCase)

  def groupPredicate(r: AdmDivision) = groupNames.exists(r.name.toUpperCase.startsWith)

  def skipGroups(regions: Seq[AdmDivision]): Seq[AdmDivision] = {
    regions flatMap {
      _ match {
        case r if groupPredicate(r) => skipGroups(r.regions)
        case r => Seq(r)
      }
    }
  }

  def shortCode(s: String, level: Int) = {
    level match {
      case 2 => s.take(2)
      case 3 => s.take(5)
      case _ => s
    }
  }

  def betterName(s: String) = {
    val s1 = s.split("/").head.toLowerCase.capitalize

    s1
      .split("-").map(_.capitalize).mkString("-")
      .split(" ").map(_.capitalize).mkString(" ")
      .split("[Мм]\\.[ ]?").map(_.capitalize).mkString("м. ")
      .replaceFirst("^[Мм]\\.[ ]?", "")
      .replaceAll("Область$", "область")
      .replaceAll("Район$", "район")
      .replaceAll("’", "'")
  }

  def withBetterName(r: Region) = r.copy(name = betterName(r.name))

  def main(args: Array[String]): Unit = {
    println(regions(() => Some(Country.Ukraine)))
  }
}