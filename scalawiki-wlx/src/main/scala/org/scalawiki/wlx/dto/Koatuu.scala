package org.scalawiki.wlx.dto

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Koatuu {

  def regionReads(level: Int): Reads[Region] = (
    (__ \ "code").read[String] and
      (__ \ "name").read[String].map(betterName) and
      (__ \ ("level" + level))
        .lazyReadNullable(Reads.seq[Region](regionReads(level + 1)))
        .map(_.getOrElse(Nil)).map(_.filterNot(groupPredicate)) and
      Reads.pure(() => None)
    ) (Region)

  def regions(parent: () => Option[AdmDivision] = () => None): Seq[Region] = {
    val stream = getClass.getResourceAsStream("/koatuu.json")
    val json = Json.parse(stream)

    implicit val level2Reads: Reads[Region] = regionReads(2)
    val raw = (json \ "level1").as[Seq[Region]]

    raw.map { r1 =>
      r1.copy(
        code = shortCode(r1.code),
        regions = r1.regions
          .map(r2 => r2.copy(code = shortCode(r2.code, 5)))
          .map(r2 => r2.copy(parent = () => Some(r1))),
        parent = parent
      )
    }
  }

  val groupNames = Seq("Міста обласного підпорядкування", "Міста", "Райони", "Селища міського типу").map(_.toUpperCase)

  def groupPredicate(r: Region) = groupNames.exists(r.name.toUpperCase.startsWith)

  def shortCode(s: String, init: Int = 2) = s.take(init)

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