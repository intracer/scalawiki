package org.scalawiki.wlx

import org.scalawiki.wlx.dto.{Country, Monument}

object RegionFixer {

  def fixLists(monumentDb: MonumentDB) {
    ListUpdater.updateLists(monumentDb, new RegionFixerUpdater(monumentDb.country))
  }
}

class RegionFixerUpdater(country: Country) extends MonumentUpdater {

  val oblasts = country.regions.filter(adm => !Set("Київ", "Севастополь").contains(adm.name))
  val raions = oblasts.flatMap(_.regions).filter(_.name.endsWith("район"))
  val raionNames = raions.map(_.name).toSet

  def updatedParams(monument: Monument): Map[String, String] = {
    Map()
  }

  def needsUpdate(m: Monument): Boolean = {
    raionNames.contains(m.cityName) && m.place.exists { p =>
      p.trim.nonEmpty &&
        country.byIdAndName(m.regionId, p.split(",").head).size == 1
    }
  }
}