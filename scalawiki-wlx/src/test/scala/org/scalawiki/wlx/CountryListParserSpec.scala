package org.scalawiki.wlx

import org.scalawiki.dto.markup.Table
import org.scalawiki.util.TestUtils._
import org.scalawiki.wlx.dto._
import org.scalawiki.wlx.dto.lists.ListConfig
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

    "parse WLE UA" in {
      CountryParser.fromCategoryName("Category:Images from Wiki Loves Earth 2015 in Ukraine") === Some(
          Contest(ContestType.WLE, Country.Ukraine, 2015,
            uploadConfigs = Seq(
              UploadConfig(
                campaign = "wle-ua",
                listTemplate = "ВЛЗ-рядок",
                fileTemplate = "UkrainianNaturalHeritageSite",
                listConfig = ListConfig.WleUa
      )))
      )
    }

    "parse WLM UA" in {
      CountryParser.fromCategoryName("Category:Images from Wiki Loves Monuments 2015 in Ukraine") === Some(
        Contest(ContestType.WLM, Country.Ukraine, 2015,
          uploadConfigs = Seq(
            UploadConfig(
              campaign = "wlm-ua",
              listTemplate = "ВЛП-рядок",
              fileTemplate = "Monument Ukraine",
              listConfig = ListConfig.WlmUa
            )))
      )
    }


  }

  "table parser" should {

    "not parse no table" in {
      CountryParser.parse("nothing useful") === Seq.empty
    }

    "not parse bad table" in {
      val wiki = new Table(Seq("Pigs", "Dogs"), Seq(Seq("1", "2"))).asWiki
      CountryParser.parse(wiki) === Seq.empty
    }

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
        Country.Ukraine.withoutLangCodes
      )
    }

    "parse wlm 2016" in {
      val wiki = resourceAsString("/org/scalawiki/wlx/wlm_2016_participating.wiki")

      val contests = CountryParser.parse(wiki)

      val countries = contests.map(_.country.withoutLangCodes)

      contests.map(_.contestType).toSet == Set(ContestType.WLM)
      contests.map(_.year).toSet == Set(2016)

      countries === Seq(
        Country("DZ", "Algeria"),
        Country("", "Andorra and Catalan Areas"),
        Country("AZ", "Azerbaijan"),
        Country("BD", "Bangladesh"),
        Country("", "Belgium & Luxembourg"),
        Country("BR", "Brazil"),
        Country("BG", "Bulgaria"),
        Country("CM", "Cameroon"),
        Country("GR", "Greece"),
        Country("EG", "Egypt"),
        Country("FR", "France"),
        Country("GE", "Georgia"),
        Country("GH", "Ghana"),
        Country("IR", "Iran"),
        Country("IE", "Ireland"),
        Country("IL", "Israel"),
        Country("IT", "Italy"),
        Country("LV", "Latvia"),
        Country("MY", "Malaysia"),
        Country("NP", "Nepal"),
        Country("NG", "Nigeria"),
        Country("PK", "Pakistan"),
        Country("PA", "Panama"),
        Country("PE", "Peru"),
        Country("RU", "Russia"),
        Country("RS", "Serbia"),
        Country("SK", "Slovakia"),
        Country("KR", "South Korea"),
        Country("ES", "Spain"),
        Country("SE", "Sweden"),
        Country("TH", "Thailand"),
        Country("TN", "Tunisia"),
        Country.Ukraine.withoutLangCodes,
        Country("", "the United Kingdom"),
        Country("", "Esperantujo")
      )
    }

  }
}
