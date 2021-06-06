package org.scalawiki.wlx.dto

import com.github.tototoshi.csv.CSVReader
import org.scalawiki.wlx.dto.Koatuu.{betterName, shortCode}

import java.io.InputStreamReader

object KatotthTypes extends RegionTypes {

  override val regionTypes = Seq(
    RegionType("O", Seq("область"), Some(" область"), Set("Автономна Республіка Крим")),
    RegionType("K", Seq("місто")), // що має спеціальний статус;
    RegionType("Р", Seq("район")),
    RegionType("Н", Seq("громада")),
    RegionType("М", Seq("місто", "м.")),
    RegionType("Т", Seq("селище міського типу", "смт")),
    RegionType("С", Seq("село", "c.")),
    RegionType("Х", Seq("селище", "с-ще")),
    RegionType("В", Seq("район")), // в місті
  )

  override val codeToType = groupTypes(regionTypes)

  def shortRegionCode(code: String, regionType: Option[RegionType]): String = {
    if (regionType.exists(rt => Set("O", "K").contains(rt.code))) {
      code.take(2)
    } else code
  }

}

object Katotth {

  def toFlat(row: Seq[String]): AdmDivisionFlat = {
    val mainLevels = row.take(4).filterNot(_.isEmpty).map(_.replace("UA", "")).distinct
    val codes = mainLevels.zipWithIndex.map { case (c, level) => shortCode(c, level + 2) }
    val additional = Seq(row(4)).filterNot(_.isEmpty).map(mainLevels.last + _.replace("UA", ""))
    AdmDivisionFlat(codeLevels = codes ++ additional,
      name = betterName(row(6)),
      regionType = KatotthTypes.codeToType.get(row(5))
    )
  }

  def readCsv: Seq[List[String]] = {
    val csv = CSVReader.open(new InputStreamReader(getClass.getResourceAsStream("/katotth.csv")))
    csv.all().tail
  }

  def regions(parent: () => Option[AdmDivision] = () => None, size: Int = 1): Seq[AdmDivision] = {
    AdmDivisionFlat.makeHierarchy(readCsv.map(toFlat), parent)
  }
}
