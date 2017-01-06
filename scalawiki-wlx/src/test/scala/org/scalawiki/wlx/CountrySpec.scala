package org.scalawiki.wlx

import org.scalawiki.wlx.dto.Country
import org.specs2.mutable.Specification

class CountrySpec extends Specification {

  "form Java locale" should {

    "contain Bulgaria" in {
      Country.byCode("BG") === Some(new Country("BG", "Bulgaria", Seq("bg")))
      Country.byCode("bg") === Some(new Country("BG", "Bulgaria", Seq("bg")))
    }

    "contain Switzerland" in {
      Country.byCode("CH") === Some(new Country("CH", "Switzerland", Seq("fr", "de", "it")))
      Country.byCode("ch") === Some(new Country("CH", "Switzerland", Seq("fr", "de", "it")))
    }
  }

  "with custom countries" should {
    "contain Ukraine" in {
      Country.byCode("UA") === Some(Country.Ukraine)
      Country.byCode("ua") === Some(Country.Ukraine)
    }

    "not contain dnr" in {
      Country.byCode("dnr") === None
    }
  }
}
