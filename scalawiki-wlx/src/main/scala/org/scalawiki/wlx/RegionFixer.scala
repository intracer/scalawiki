package org.scalawiki.wlx

import org.scalawiki.wlx.dto.{AdmDivision, Contest, Country, Monument}

object RegionFixer {

  def fixLists(monumentDb: MonumentDB): Unit = {
    ListUpdater.updateLists(monumentDb, new RegionFixerUpdater(monumentDb))
  }
}

class RegionFixerUpdater(monumentDb: MonumentDB) extends MonumentUpdater {

  val contest: Contest = monumentDb.contest
  val country: Country = contest.country
  val oblasts: Seq[AdmDivision] =
    country.regions.filter(adm => !Set("Київ", "Севастополь").contains(adm.name))
  val raions: Seq[AdmDivision] = oblasts.flatMap(_.regions).filter(_.name.endsWith("район"))
  val raionNames: Set[String] = raions.map(_.name).toSet

  val nameParam: String = contest.uploadConfigs.head.listConfig.namesMap("city")
  val maxIndex = 2

  def updatedParams(m: Monument): Map[String, String] = {
    getIndex(m)
      .flatMap { index =>
        val fixedPlace = m.place.flatMap(_.split(",").toList.lift(index))
        fixedPlace.map { place =>
          Map(nameParam -> place)
        }
      }
      .getOrElse(Map.empty)
  }

  def needsUpdate(m: Monument): Boolean = {
    (monumentDb.getAdmDivision(m.id).isEmpty || raionNames.contains(
      m.cityName
    )) && getIndex(m).nonEmpty
  }

  def getIndex(m: Monument): Option[Int] = {
    m.place.flatMap { p =>
      val namesList = p.split(",").toBuffer
      (0 to maxIndex).find { index =>
        namesList.lift(index).exists { c =>
          country.byIdAndName(m.id.take(6), c, m.cityType).size == 1
        }
      }
    }
  }
}
