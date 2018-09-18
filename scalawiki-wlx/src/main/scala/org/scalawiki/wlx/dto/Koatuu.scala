package org.scalawiki.wlx.dto

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Koatuu {

  implicit lazy val regionReads: Reads[Region] = (
    (__ \ "code").read[String] and
      (__ \ "name").read[String] and
      (__ \ "level2").lazyReadNullable(Reads.seq[Region](regionReads)).map(_.getOrElse(Nil))
    ) (Region)

  def regions: Seq[Region] = {
    val stream = getClass.getResourceAsStream("/koatuu.json")
    val json = Json.parse(stream)

    val raw = (json \ "level1").as[Seq[Region]]

    raw.map { r =>
      r.copy(
        code = shortCode(r.code),
        name = betterName(r.name),
        regions = r.regions.map(withBetterName)
      )
    }
  }

  def shortCode(s: String) =
    s.take(2)

  def betterName(s: String) = {
    def capitalizeRegion(s: String) = {
      Seq("Міста обласного підпорядкування", "Міста", "Райони")
        .filter(s.startsWith)
        .map { prefix =>
          prefix + " " + s.replaceFirst(prefix + " ", "").capitalize
        }.headOption.getOrElse(s)
    }

    val s1 = s.split("/").head
      .toLowerCase.capitalize

    capitalizeRegion(s1)
      .split("-").map(_.capitalize).mkString("-")
      .split("[Мм]\\.[ ]?").map(_.capitalize).mkString("м. ")
      .replaceFirst("^[Мм]\\.[ ]?", "")
      .replace("республіка", "Республіка")
      .replace("республіки", "Республіки")
      .replace("крим", "Крим")


  }

  def withBetterName(r: Region) = r.copy(name = betterName(r.name))

  def main(args: Array[String]): Unit = {
    println(regions)
  }

}