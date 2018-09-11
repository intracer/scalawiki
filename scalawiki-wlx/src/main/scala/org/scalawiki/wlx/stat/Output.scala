package org.scalawiki.wlx.stat

import org.scalawiki.dto.Image
import org.scalawiki.wlx.{ImageDB, MonumentDB}

class Output {

  def monumentsByType(/*imageDbs: Seq[ImageDB], totalImageDb: ImageDB,*/ monumentDb: MonumentDB) = {
    val regions = monumentDb.contest.country.regionById

    for ((typ, size) <- monumentDb._byType.mapValues(_.size).toSeq.sortBy(-_._2)) {
      val byRegion = monumentDb._byTypeAndRegion(typ)

      val regionStat = byRegion.toSeq.sortBy(-_._2.size).map {
        case (regionId, monuments) =>
          val byReg1 = s"${regions(regionId)}: ${monuments.size}"

          val byReg2 = if (byRegion.size == 1) {
            val byReg2Stat = monuments.groupBy(m => m.id.substring(0, 6))

            byReg2Stat.toSeq.sortBy(-_._2.size).map {
              case (regionId2, monuments2) =>
                s"$regionId2: ${monuments2.size}"
            }.mkString("(", ", ", ")")
          } else ""

          byReg1 + byReg2
      }.mkString(", ")
      println(s"$typ: ${monumentDb._byType(typ).size}, $regionStat")
    }
  }

  def galleryByRegionAndId(monumentDb: MonumentDB, authorImageDb: ImageDB, oldImageDb: ImageDB): String = {
    val country = monumentDb.contest.country
    val regionIds = country.regionIds.filter(id => authorImageDb.idsByRegion(id).nonEmpty)

    regionIds.map {
      regionId =>
        val regionName = country.regionById(regionId).name
        val regionHeader = s"== [[:uk:Вікіпедія:Вікі любить Землю/$regionName|$regionName]] ==\n"
        val ids = authorImageDb.idsByRegion(regionId)

        val newIds = ids -- oldImageDb.ids
        val oldIds = ids -- newIds

        regionHeader +
          gallery("new ids", newIds, authorImageDb, monumentDb) +
          gallery("old ids", oldIds, authorImageDb, monumentDb)

    }.mkString("\n")
  }

  private def gallery(header: String, ids: Set[String], imageDb: ImageDB, monumentDb: MonumentDB) = {
    if (ids.nonEmpty) {
      s"=== $header ===\n" +
        ids.map {
          id =>
            val images = imageDb.byId(id).map(_.title).sorted
            s"==== $id ====\n" +
              s"${monumentDb.byId(id).get.name.replace("[[", "[[:uk:")}\n" +
              Image.gallery(images)
        }.mkString("\n")
    } else ""
  }
}
