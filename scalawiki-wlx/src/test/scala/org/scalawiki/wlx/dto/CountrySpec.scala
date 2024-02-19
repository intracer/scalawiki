package org.scalawiki.wlx.dto

import org.specs2.mutable.Specification

class CountrySpec extends Specification {

  "form Java locale" should {

    "contain Bulgaria" in {
      Country.byCode("BG") === Some(new Country("BG", "Bulgaria", Seq("bg")))
      Country.byCode("bg") === Some(new Country("BG", "Bulgaria", Seq("bg")))
    }

    "contain Switzerland" in {
      val Switzerland = Country.byCode("CH").get
      Switzerland === Country.byCode("ch").get

      Switzerland.code === "CH"
      Switzerland.name === "Switzerland"
      Switzerland.languageCodes.toSet
        .intersect(Set("fr", "de", "it")) === Set("fr", "de", "it")
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
