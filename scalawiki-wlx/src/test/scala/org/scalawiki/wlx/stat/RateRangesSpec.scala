package org.scalawiki.wlx.stat

import com.typesafe.config.ConfigFactory
import org.scalawiki.wlx.dto.{Contest, ContestType, Country}
import org.scalawiki.wlx.{ImageDB, MonumentDB}
import org.specs2.mutable.Specification

class RateRangesSpec extends Specification {
  "RateRanges" should {
    "parse no ranges" in {
      RateRanges(ConfigFactory.parseString("")).rangeMap === Map.empty
    }

    "parse one range" in {
      RateRanges(ConfigFactory.parseString("""{"0-0":9}""")).rangeMap === Map(((0, 0), 9))
    }

    "parse several ranges" in {
      RateRanges(ConfigFactory.parseString("""{"0-0": 9, "1-3": 3, "4-9": 1}""")).rangeMap === Map(
        ((0, 0), 9),
        ((1, 3), 3),
        ((4, 9), 1)
      )
    }

    "do not allow reversed range" in {
      val exception = new IllegalArgumentException("Invalid ends order in range 2-1: 2 > 1")
      RateRanges(ConfigFactory.parseString("""{"0-0": 9, "2-1": 3}""")) must throwA(exception)
    }

    "do not allow overlaps" in {
      val exception = new IllegalArgumentException("Ranges (0,0) and (0,1) overlap")
      RateRanges(ConfigFactory.parseString("""{"0-0": 9, "0-1": 3}""")) must throwA(exception)
    }
  }
}
