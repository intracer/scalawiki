package org.scalawiki.wlx

import org.scalawiki.util.TestUtils._
import org.scalawiki.wlx.dto.{ContestType, Country}
import org.specs2.mutable.Specification

class CountryListParserSpec extends Specification {

  "parser" should {
    "parse wle 2016" in {
      val wiki = resourceAsString("/org/scalawiki/wlx/wle_2016_participating.wiki")

      val contests = CountryParser.parse(wiki)

      val countries = contests.map(_.country.copy(languageCodes = Seq.empty))

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
