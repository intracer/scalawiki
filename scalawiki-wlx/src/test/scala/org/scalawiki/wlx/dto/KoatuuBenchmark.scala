package org.scalawiki.wlx.dto

import org.scalameter.{Bench, Gen}

object KoatuuBenchmark extends Bench.LocalTime {

  measure method "regions" in {
    using(Gen.unit("koatuu")) in { _ =>
      Koatuu.regions().size
    }
  }
}