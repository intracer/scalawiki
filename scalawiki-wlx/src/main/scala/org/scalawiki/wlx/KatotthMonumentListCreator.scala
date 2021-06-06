package org.scalawiki.wlx

import org.scalawiki.wlx.dto.{Contest, Country, Katotth, Koatuu}
import org.scalawiki.wlx.query.MonumentQuery

object KatotthMonumentListCreator {

  val UkraineKatotth: Country = new Country("UA", "Ukraine", Seq("uk"), Katotth.regions(() => Some(UkraineKatotth)))
  val regionsKatotth = UkraineKatotth.regions
  val katotthMap = UkraineKatotth.mapByCode

  val UkraineKoatuu: Country = new Country("UA", "Ukraine", Seq("uk"), Koatuu.regionsNew(() => Some(UkraineKoatuu)))
  val regionsKoatuu = UkraineKoatuu.regions
  val koatuuMap = UkraineKoatuu.mapByCode


  def main(args: Array[String]): Unit = {
    val contest = Contest.WLMUkraine(2021)
    val query = MonumentQuery.create(contest)
    val monumentDB = MonumentDB.getMonumentDb(contest, query)
    val placeByMonumentId = monumentDB.placeByMonumentId
    monumentDB.monuments.map { m =>
      val koatuuOpt  = placeByMonumentId.get(m.id)
      val katotth = koatuuOpt.map { koatuu =>
        Katotth.toKatotth(koatuu).map(katotthMap.apply).maxBy(_.level)
      }

      m
    }
  }
}
