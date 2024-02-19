package org.scalawiki.wlx.dto

import org.specs2.mutable.Specification
import org.scalawiki.util.TestUtils.resourceAsString
import org.scalawiki.wlx.{KatotthMonumentListCreator, MonumentDB}
import org.scalawiki.wlx.dto.lists.ListConfig.WlmUa

class KoatuuToKatotthSpec extends Specification {
  val contest = Contest.WLMUkraine(2020)

  val UkraineKatotth: Country = new Country(
    "UA",
    "Ukraine",
    Seq("uk"),
    Katotth.regions(() => Some(UkraineKatotth))
  )
  val regionsKatotth = UkraineKatotth.regions
  val katotthMap = UkraineKatotth.mapByCode

  val UkraineKoatuu: Country = new Country(
    "UA",
    "Ukraine",
    Seq("uk"),
    Koatuu.regionsNew(() => Some(UkraineKoatuu))
  )
  val regionsKoatuu = UkraineKoatuu.regions
  val koatuuMap = UkraineKoatuu.mapByCode

  "converter" should {

    "map cities" in {
      val burshtynWiki = resourceAsString("/org/scalawiki/wlx/Burshtyn.wiki")
      val page =
        "Вікіпедія:Вікі любить пам'ятки/Івано-Франківська область/Бурштин"
      val monuments = Monument
        .monumentsFromText(burshtynWiki, page, WlmUa.templateName, WlmUa)
        .toSeq
      monuments.size === 21

      val mdb = new MonumentDB(contest, monuments)
      val mapping = KatotthMonumentListCreator.getMapping(mdb)
      mapping.size === 21
      mapping.flatMap(_.koatuu).toSet === Set("26103")
      mapping.flatMap(_.katotth.map(_.name)).toSet === Set("Бурштин")
    }

    //    "find Pereyaslav in koatuu" in {
    //      val kyivOblast = UkraineKoatuu.regions.find(_.name.startsWith("Київська")).get
    //      val kyivOblastChildren =  kyivOblast.regions
    //      ok
    //    }

//    "find Ternopilsky raion in koatuu" in {
//      val ternopilOblast = UkraineKatotth.regions.find(_.name.startsWith("Тернопільська")).get
//      val ternopilOblastChildren = ternopilOblast.regions
//      ok
//    }

//    "map every place" in {
//      val mapping = Katotth.toKoatuu
//      for ((katotth, koatuu) <- mapping) yield {
//        if (!katotthMap.contains(katotth)) {
//          val seq = katotthMap.keySet.filter(katotth.startsWith).toSeq
//          if (seq.isEmpty) {
//            failure(s"$katotth is absent in katotth")
//          }
//          val shortCode = seq.maxBy(_.length)
//        }
//        if (!koatuuMap.contains(koatuu) && koatuu != "Новий район") {
//          val seq = koatuuMap.keySet.filter(koatuu.startsWith).toSeq
//          if (seq.isEmpty) {
//            failure(s"$koatuu is absent in koatuu")
//          }
//          val shortCode = seq.maxBy(_.length)
//        }
//      }
//      ok
//    }

    //    "work with visem region" in {
    //      regionsKatotth.size === 27
    //      regionsKoatuu.size === 27
    //
    //      val katotthOblast = regionsKatotth(1)
    //      val koatuuOblast = regionsKoatuu(1)
    //
    //      katotthOblast.fullName === koatuuOblast.name
    //      katotthOblast.code === koatuuOblast.code
    //
    //      val katotthRaion = katotthOblast.regions.head
    //      val katotthRaionName = katotthRaion.fullName
    //      val koatuuRaion = koatuuOblast.regions.find(_.name == katotthRaionName).get
    //      val koatuuRaionCode = koatuuRaion.code
    //
    //      katotthRaion.fullName === koatuuRaion.name
    //
    //      val oths = katotthRaion.regions
    //      val koatuu111 = koatuuRaion.regions
    //
    //      val othsSubregions = oths.flatMap(_.regions)
    //      val koatuuSubregions = othsSubregions.map { othPlace =>
    //        val koatuuPlace = koatuuOblast.byId(Katotth.toKoatuu(othPlace.code))
    //        othPlace -> koatuuPlace
    //      }
    //
    //      val found = koatuuSubregions.filter { case (_, koatuu) => koatuu.size == 1 }
    //      val many = koatuuSubregions.filter { case (_, koatuu) => koatuu.size > 1 }
    //      val no = koatuuSubregions.filter { case (_, koatuu) => koatuu.isEmpty }
    //      val all = koatuuSubregions.size
    //      all === found
    //    }
  }

}
