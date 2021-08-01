package org.scalawiki.wlx.dto

import com.github.tototoshi.csv.CSVReader
import org.scalawiki.wlx.dto.Koatuu.{betterName, shortCode}

import java.io.InputStreamReader

object KatotthTypes extends RegionTypes {

  override val regionTypes = Seq(
    RegionType("O", Seq("область"), Some(" область"), Set("Автономна Республіка Крим")),
    RegionType("K", Seq("місто")), // що має спеціальний статус;
    RegionType("P", Seq("район"), Some(" район")),
    RegionType("H", Seq("громада"), Some(" громада")),
    RegionType("M", Seq("місто", "м.")),
    RegionType("T", Seq("селище міського типу", "смт")),
    RegionType("C", Seq("село", "c.")),
    RegionType("X", Seq("селище", "с-ще")),
    RegionType("B", Seq("район"), Some(" район")) // в місті
  )

  override val codeToType = groupTypes(regionTypes)

  def shortRegionCode(code: String, regionType: Option[RegionType]): String = {
    if (regionType.exists(rt => Set("O", "K").contains(rt.code))) {
      code.take(2)
    } else code
  }

}

object Katotth {

  lazy val toKoatuu: Map[String, String] = {
    readCsv("katotth_koatuu").map { row =>
      row(0).replace("UA", "") -> row(1)
    }.toMap
  }

  lazy val toKatotth: Map[String, Seq[String]] = toKoatuu.toSeq.groupBy(_._2).mapValues(_.map(_._1)).toMap

  def toFlat(row: Seq[String]): AdmDivisionFlat = {
    val mainLevels = row.take(4).filterNot(_.isEmpty).map(_.replace("UA", "")).distinct
    val codes = mainLevels.zipWithIndex.map { case (c, level) => shortCode(c, level + 2) }
    val additional = Seq(row(4)).filterNot(_.isEmpty).map(mainLevels.last + _.replace("UA", ""))
    AdmDivisionFlat(codeLevels = codes ++ additional,
      name = betterName(row(6)),
      regionType = KatotthTypes.codeToType.get(row(5))
    )
  }

  def readCsv(filename: String): Seq[List[String]] = {
    val csv = CSVReader.open(new InputStreamReader(getClass.getResourceAsStream(s"/$filename.csv")))
    csv.all().tail
  }

  def regions(parent: () => Option[AdmDivision] = () => None, size: Int = 1): Seq[AdmDivision] = {
    val flat = readCsv("katotth").map(toFlat)
    AdmDivisionFlat.makeHierarchy(flat, parent)
  }
}
