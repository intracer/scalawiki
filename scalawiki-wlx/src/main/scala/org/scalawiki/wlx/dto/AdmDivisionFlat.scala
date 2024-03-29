package org.scalawiki.wlx.dto

import org.scalawiki.wlx.dto.Koatuu.{betterName, shortCode, skipGroups}

case class AdmDivisionFlat(
    codeLevels: Seq[String],
    name: String,
    regionType: Option[RegionType]
) {
  def code: String = codeLevels.last

  def toHierarchy(
      regions: Seq[AdmDivision],
      parent: () => Option[AdmDivision]
  ): AdmDivision = {
    val hier = AdmDivision(
      code,
      name,
      regions,
      parent,
      regionType,
      parent().map(p => Option(p).fold(1)(_.level + 1)).getOrElse(0)
    )
    regions.map(_.withParents(() => Some(hier)))
    hier
  }

}

object AdmDivisionFlat {
  def apply(
      l1: String,
      l2: String,
      l3: String,
      l4: String,
      name: String,
      regionType: Option[RegionType]
  ): AdmDivisionFlat = {
    val levels = Seq(l1, l2, l3, l4).filterNot(_.isEmpty)
    val codes = levels.zipWithIndex.map { case (c, level) =>
      shortCode(c, level + 2)
    }
    AdmDivisionFlat(codes, betterName(name), regionType)
  }

  def makeHierarchy(
      flat: Seq[AdmDivisionFlat],
      parent: () => Option[AdmDivision] = () => None,
      level: Int = 0
  ): Seq[AdmDivision] = {
    flat
      .groupBy(_.codeLevels(level))
      .map { case (code, regions) =>
        val (topList, subRegions) =
          regions.partition(_.codeLevels.size == level + 1)
        val top = topList.head
        val children =
          skipGroups(makeHierarchy(subRegions, () => None, level + 1))
        top.toHierarchy(children, parent)
      }
      .toSeq
      .sortBy(_.code)
  }

}
