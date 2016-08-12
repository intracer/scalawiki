package org.scalawiki.wlx

import org.scalawiki.util.TestUtils._
import org.scalawiki.wlx.dto.{Contest, ContestType, Country, NoAdmDivision}
import org.specs2.mutable.Specification

class CountryListParserSpec extends Specification {

  "category name parser" should {

    "parse WLE" in {
      CountryParser.fromCategoryName("Category:Images from Wiki Loves Earth 2015") === Some(
        Contest(ContestType.WLE, NoAdmDivision(), 2015, uploadConfigs = Seq.empty)
      )
    }

    "parse WLM" in {
      CountryParser.fromCategoryName("Category:Images from Wiki Loves Monuments 2015") === Some(
        Contest(ContestType.WLM, NoAdmDivision(), 2015, uploadConfigs = Seq.empty)
      )
    }

    "parse WLE country" in {
      CountryParser.fromCategoryName("Category:Images from Wiki Loves Earth 2015 in Algeria") === Some(
        Contest(ContestType.WLE, Country("DZ", "Algeria", Seq("ar")), 2015, uploadConfigs = Seq.empty)
      )
    }

    "parse WLM country" in {
      CountryParser.fromCategoryName("Category:Images from Wiki Loves Monuments 2015 in Algeria") === Some(
        Contest(ContestType.WLM, Country("DZ", "Algeria", Seq("ar")), 2015, uploadConfigs = Seq.empty)
      )
    }
  }

  "table parser" should {
    "parse wle 2016" in {
      val wiki = resourceAsString("/org/scalawiki/wlx/wle_2016_participating.wiki")

      val contests = CountryParser.parse(wiki)

      val countries = contests.map(_.country.withoutLangCodes)

      contests.map(_.contestType).toSet == Set(ContestType.WLE)
      contests.map(_.year).toSet == Set(2016)

      countries === Seq(
        Country("DZ", "Algeria"),
        Country("AU", "Australia"),
        Country("AT", "Austria"),
        Country("AZ", "Azerbaijan"),
        Country("BR", "Brazil"),
        Country("BG", "Bulgaria"),
        Country("FR", "France"),
        Country("DE", "Germany"),
        Country("GR", "Greece"),
        Country("IQ", "Iraq"),
        Country("MD", "Moldova"),
        Country("MA", "Morocco"),
        Country("NP", "Nepal"),
        Country("PK", "Pakistan"),
        Country("RU", "Russia"),
        Country("RS", "Serbia"),
        Country("ES", "Spain"),
        Country("CH", "Switzerland"),
        Country("TH", "Thailand"),
        Country("TN", "Tunisia"),
        Country("UA", "Ukraine")
      )
    }
  }
}
