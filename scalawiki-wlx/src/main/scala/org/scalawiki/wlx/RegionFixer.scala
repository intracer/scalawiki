package org.scalawiki.wlx

import org.scalawiki.wlx.dto.{Contest, Monument}

object RegionFixer {

  def fixLists(monumentDb: MonumentDB) {
    ListUpdater.updateLists(monumentDb, new RegionFixerUpdater(monumentDb.contest))
  }
}

class RegionFixerUpdater(contest: Contest) extends MonumentUpdater {

  val country = contest.country
  val oblasts = country.regions.filter(adm => !Set("Київ", "Севастополь").contains(adm.name))
  val raions = oblasts.flatMap(_.regions).filter(_.name.endsWith("район"))
  val raionNames = raions.map(_.name).toSet

  val nameParam = contest.uploadConfigs.head.listConfig.namesMap("city")

  def updatedParams(m: Monument): Map[String, String] = {
    val fixedPlace = m.place.flatMap(_.split(",").headOption)
    fixedPlace.map { place =>
      Map(nameParam -> place)
    }.getOrElse(Map.empty)
  }

  def needsUpdate(m: Monument): Boolean = {
    raionNames.contains(m.cityName) && m.place.exists { p =>
      p.trim.nonEmpty &&
        country.byIdAndName(m.regionId, p.split(",").head).size == 1
    }
  }
}