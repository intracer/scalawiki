package org.scalawiki.wlx.stat

import org.scalawiki.wlx.stat.rating.RateConfig

import java.time.ZonedDateTime
import org.specs2.mutable.Specification

class StatParamsSpec extends Specification {

  "statistics" should {
    val thisYear = ZonedDateTime.now.getYear

    "parse campaign" in {
      val cfg = StatParams.parse(Seq("--campaign", "wlm-ua"))
      cfg === StatConfig("wlm-ua", Seq(thisYear))
    }

    "parse short campaign" in {
      val cfg = StatParams.parse(Seq("-c", "wlm-ua"))
      cfg === StatConfig("wlm-ua", Seq(thisYear))
    }

    "parse campaign with years" in {
      val cfg =
        StatParams.parse(Seq("--campaign", "wle-ua", "--year", "2015", "2016"))
      cfg === StatConfig("wle-ua", Seq(2015, 2016))
    }

    "parse short campaign with years" in {
      val cfg = StatParams.parse(Seq("-c", "wle-ua", "-y", "2015", "2016"))
      cfg === StatConfig("wle-ua", Seq(2015, 2016))
    }

    "years sorted" in {
      val cfg = StatParams.parse(
        Seq("--campaign", "wle-ua", "--year", "2016", "2014", "2015", "2012")
      )
      cfg === StatConfig("wle-ua", 2012 to 2016)
    }

    "start year" in {
      val cfg = StatParams.parse(
        Seq("--campaign", "wle-ua", "-y", "2017", "--start-year", "2012")
      )
      cfg === StatConfig("wle-ua", 2012 to 2017)
    }

    "parse new object rating" in {
      StatParams.parse(
        Seq("--campaign", "wle-ua", "--new-object-rating", "7")
      ) ===
        StatConfig(
          "wle-ua",
          Seq(thisYear),
          rateConfig = RateConfig(newObjectRating = Some(7))
        )

      StatParams.parse(
        Seq("--campaign", "wle-ua", "--new-author-object-rating", "3")
      ) ===
        StatConfig(
          "wle-ua",
          Seq(thisYear),
          rateConfig = RateConfig(newAuthorObjectRating = Some(3))
        )

      StatParams.parse(
        Seq(
          "--campaign",
          "wle-ua",
          "--new-object-rating",
          "10",
          "--new-author-object-rating",
          "5"
        )
      ) ===
        StatConfig(
          "wle-ua",
          Seq(thisYear),
          rateConfig = RateConfig(
            newObjectRating = Some(10),
            newAuthorObjectRating = Some(5)
          )
        )
    }

    "parse bonus" in {
      StatParams.parse(
        Seq("--campaign", "wle-ua", "--number-of-authors-bonus")
      ) ===
        StatConfig(
          "wle-ua",
          Seq(thisYear),
          rateConfig = RateConfig(numberOfAuthorsBonus = true)
        )

      StatParams.parse(
        Seq("--campaign", "wle-ua", "--number-of-images-bonus")
      ) ===
        StatConfig(
          "wle-ua",
          Seq(thisYear),
          rateConfig = RateConfig(numberOfImagesBonus = true)
        )

      StatParams.parse(
        Seq(
          "--campaign",
          "wle-ua",
          "--number-of-authors-bonus",
          "--number-of-images-bonus"
        )
      ) ===
        StatConfig(
          "wle-ua",
          Seq(thisYear),
          rateConfig = RateConfig(numberOfAuthorsBonus = true, numberOfImagesBonus = true)
        )
    }

    "parse gallery" in {
      StatParams.parse(Seq("--campaign", "wle-ua")) === StatConfig(
        "wle-ua",
        Seq(thisYear)
      )
      StatParams.parse(Seq("--campaign", "wle-ua", "--gallery")) === StatConfig(
        "wle-ua",
        Seq(thisYear),
        gallery = true
      )
    }

    "parse place-detection" in {
      StatParams.parse(Seq("--campaign", "wle-ua")) === StatConfig(
        "wle-ua",
        Seq(thisYear)
      )
      StatParams.parse(
        Seq("--campaign", "wle-ua", "--place-detection")
      ) === StatConfig("wle-ua", Seq(thisYear), placeDetection = true)
    }

    "parse campaign with regions" in {
      val cfg = StatParams.parse(
        Seq("--campaign", "wle-ua", "--year", "2012", "--region", "01", "02")
      )
      cfg === StatConfig("wle-ua", Seq(2012), Seq("01", "02"))
    }
  }

}
