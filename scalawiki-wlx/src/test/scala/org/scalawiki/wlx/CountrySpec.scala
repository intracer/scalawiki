package org.scalawiki.wlx

import org.scalawiki.wlx.dto.Country
import org.specs2.mutable.Specification

class CountrySpec extends Specification {

  "form Java locale" should {
    val countries = Country.fromJavaLocales

    "contain Ukraine" in {
      countries.find(_.code == "UA") === Some(new Country("UA", "Ukraine", Seq("uk")))
    }

    "contain Switzerland" in {
      countries.find(_.code == "CH") === Some(new Country("CH", "Switzerland", Seq("fr", "de", "it")))
    }
  }

  "with custom countries" should {
    "contain Ukraine" in {
      val country: Country = Country.byCode("ua")
      country === Country.Ukraine
      country.regions.size === 27
    }
  }

}
