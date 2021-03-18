package org.scalawiki.wlx.dto

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class RegionType(code: String, names: Seq[String])

object RegionTypes {

  val types = Seq(
    RegionType("Р", Seq("Район")),
    RegionType("Т", Seq("Селище міського типу", "смт")),
    RegionType("С", Seq("Село", "c.")),
    RegionType("Щ", Seq("селище", "с-ще")),
    RegionType("М", Seq("місто", "м."))

  )

  val codeToType = types.groupBy(_.code).mapValues(_.head)
  def nameToType(name: String) = types.filter(t => t.names.map(_.toLowerCase).toSet.exists(name.toLowerCase.contains))
}

object Koatuu {

  def regions(parent: () => Option[AdmDivision] = () => None): Seq[AdmDivision] = {
    val stream = getClass.getResourceAsStream("/koatuu.json")
    val json = Json.parse(stream)

    implicit val level2Reads: Reads[AdmDivision] = regionReads(2, parent)
    (json \ "level1").as[Seq[AdmDivision]].map(_.withParents(parent))
  }

  def regionReads(level: Int, parent: () => Option[AdmDivision] = () => None): Reads[AdmDivision] = (
    (__ \ "code").read[String].map(c => shortCode(c, level)) and
      (__ \ "name").read[String].map(betterName) and
      (__ \ ("level" + level))
        .lazyReadNullable(Reads.seq[AdmDivision](regionReads(level + 1)))
        .map(_.getOrElse(Nil)).map(skipGroups) and
      Reads.pure(parent) and
      (__ \ "type").readNullable[String].map(_.flatMap(RegionTypes.codeToType.get))
    ) (AdmDivision.apply(_, _, _, _, _))

  val groupNames = Seq("Міста обласного підпорядкування", "Міста", "Райони", "Селища міського типу", "Населені пункти")
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
  }

  def withBetterName(r: Region) = r.copy(name = betterName(r.name))

  def main(args: Array[String]): Unit = {
    println(regions(() => Some(Country.Ukraine)))
  }
}