package org.scalawiki.wlx.stat

import com.typesafe.config.{Config, ConfigFactory}
import org.scalawiki.wlx.{ImageDB, MonumentDB}
import org.scalawiki.wlx.dto.{Contest, ContestType, Country}
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
        ((4, 9), 1),
      )
    }

    "do not allow overlaps" in {
      val exception = new IllegalArgumentException("Ranges (0,0) and (0,1) overlap")
      RateRanges(ConfigFactory.parseString("""{"0-0": 9, "0-1": 3}""")) must throwA(exception)
    }
  }

  "Rater config" should {
    val contest = Contest(ContestType.WLM, Country.Ukraine, 2020)
    val monumentDb = Some(new MonumentDB(contest, Nil))
    val imageDb = new ImageDB(contest, Nil, monumentDb)
    val contestStat = ContestStat(contest = contest, startYear = 2019, monumentDb = monumentDb,
      currentYearImageDb = Some(imageDb), totalImageDb = Some(imageDb))

    "parse wlm 2020" in {
      val rater = Rater.fromConfig(contestStat, ConfigFactory.load("wlm_ua.conf"))
      rater.asInstanceOf[RateSum].raters.size === 3
    }
  }
}
