package org.scalawiki.wlx

import org.scalawiki.wlx.dto.{AdmDivision, Contest, Country, Katotth, Koatuu}
import org.scalawiki.wlx.query.MonumentQuery

case class Koatuu2Katotth(koatuu: Option[String], katotth: Option[AdmDivision], monumentIds: Seq[String])

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
    val sequence = monumentDB.monuments.map { m =>
      val koatuuOpt = placeByMonumentId.get(m.id)
      val katotthOpt = koatuuOpt.flatMap { koatuu =>
        val candidates = Katotth.toKatotth.getOrElse(koatuu, Nil).flatMap(katotthMap.get)
        if (candidates.nonEmpty) Some(candidates.maxBy(_.level)) else None
      }

      Koatuu2Katotth(koatuuOpt, katotthOpt, List(m.id))
    }
    val parentCodes = Set("H", "P", "K", "O")
    val grouped = sequence.filter(_.katotth.nonEmpty).groupBy { k2k =>
      val k = k2k.katotth.get
      var groupK = k

      while (!groupK.regionType.exists(rt => parentCodes.contains(rt.code)) && groupK.parent().nonEmpty) {
        groupK = groupK.parent().get
      }
      groupK
    }

    println(grouped.size)
  }
}
