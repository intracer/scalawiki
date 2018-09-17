package org.scalawiki.wlx.dto

import org.specs2.mutable.Specification

class KoatuuSpec extends Specification {

  "Koatuu" should {

    val regions = Koatuu.regions

    "contain level1" in {
      regions.map(_.copy(regions = Nil)) === Country.Ukraine.regions
    }
  }
}
