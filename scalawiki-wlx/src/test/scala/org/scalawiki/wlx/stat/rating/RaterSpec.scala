package org.scalawiki.wlx.stat.rating

import com.typesafe.config.ConfigFactory
import org.scalawiki.wlx.dto.ContestType.{WLE, WLM}
import org.scalawiki.wlx.dto.{Contest, ContestType, Country}
import org.scalawiki.wlx.stat.ContestStat
import org.scalawiki.wlx.stat.rating.RaterSpec.loadRater
import org.scalawiki.wlx.{ImageDB, MonumentDB}
import org.specs2.mutable.Specification

class RaterSpec extends Specification {

  "Rater" should {

    "parse wlm 2020" in {
      val rater = loadRater(WLM, 2020)

      rater match {
        case RateSum(
              _,
              List(
                NumberOfMonuments(_, 1, _),
                NumberOfAuthorsBonus(_, numberOfAuthorsRates),
                NumberOfImagesInPlaceBonus(_, numberOfImagesInPlaceRates)
              )
            ) =>
          numberOfAuthorsRates === RateRanges(
            Map((0, 0) -> 10.0, (1, 3) -> 6.0, (4, 6) -> 3.0, (7, 9) -> 1.0)
          )
          numberOfImagesInPlaceRates === RateRanges(
            Map((0, 0) -> 10.0, (1, 3) -> 6.0, (4, 9) -> 3.0, (10, 49) -> 1.0)
          )
        case other => ko(s"unexpected rater $other")
      }

    }

    "parse wlm 2023" in {
      val rater = loadRater(WLM, 2023)

      rater match {
        case RateSum(
              _,
              List(
                NumberOfMonuments(_, 1, _),
                NumberOfAuthorsBonus(_, numberOfAuthorsRates),
                NumberOfImagesInPlaceBonus(_, numberOfImagesInPlaceRates)
              )
            ) =>
          numberOfAuthorsRates === RateRanges(
            Map((0, 0) -> 10.0, (1, 3) -> 6.0, (4, 6) -> 3.0, (7, 9) -> 1.0)
          )
          numberOfImagesInPlaceRates === RateRanges(
            Map((0, 0) -> 10.0, (1, 3) -> 6.0, (4, 9) -> 3.0, (10, 49) -> 1.0)
          )
        case other => ko(s"unexpected rater $other")
      }
    }

    "parse wlm 2026" in {
      val rater = loadRater(WLM, 2026)

      rater match {
        case RateSum(
              _,
              List(
                NumberOfMonuments(_, 1, regionRates),
                NumberOfAuthorsBonus(_, numberOfAuthorsRates),
                NumberOfImagesInPlaceBonus(_, numberOfImagesInPlaceRates),
                NumberOfInteriorImagesBonus(_, numberOfInteriorImagesRates),
                OldPhotosBonus(_, oldPhotosBonus, oldPhotosBeforeDate)
              )
            ) =>
          regionRates === Map(
            "01" -> 10.0,
            "14" -> 10.0,
            "23" -> 10.0,
            "44" -> 10.0,
            "65" -> 10.0,
            "85" -> 10.0
          )
          numberOfAuthorsRates === RateRanges(
            Map((0, 0) -> 12.0, (1, 3) -> 6.0, (4, 6) -> 3.0, (7, 9) -> 1.0)
          )
          numberOfImagesInPlaceRates === RateRanges(
            Map((0, 0) -> 10.0, (1, 3) -> 6.0, (4, 9) -> 3.0, (10, 49) -> 1.0)
          )
          numberOfInteriorImagesRates === RateRanges(
            Map((0, 0) -> 5.0, (1, 5) -> 3.0)
          )
          oldPhotosBonus === 5.0
          oldPhotosBeforeDate === java.time.LocalDate.of(2020, 1, 1)
        case other => ko(s"unexpected rater $other")
      }
    }

    "parse wle 2020" in {
      val rater = loadRater(WLE, 2020)

      rater match {
        case RateSum(
              _,
              List(
                NumberOfMonuments(_, 1, _),
                NumberOfAuthorsBonus(_, numberOfAuthorsRates)
              )
            ) =>
          numberOfAuthorsRates === RateRanges(
            Map((0, 0) -> 9.0, (1, 3) -> 3.0, (4, 9) -> 1.0),
            sameAuthorZeroBonus = true
          )
        case other => ko(s"unexpected rater $other")
      }
    }

//    "05-101-0380" in {
//      val monumentId = "05-101-0380"
//      val rater = Rater.fromConfig(contestStat, ConfigFactory.load("wlm_ua.conf"))
//
//      val rate = rater.rate(monumentId, "Author")
//      val description = rater.explain(monumentId, "Author")
//
//      description === ""
//      rate === 0
//    }

  }

}

object RaterSpec {
  private def loadRater(contestType: ContestType, year: Int) = {
    Rater.fromConfig(
      makeContestStat(contestType, year),
      ConfigFactory.load(s"${contestType.code}_ua.conf")
    )
  }

  private def makeContestStat(contestType: ContestType, year: Int) = {
    val contest = Contest(contestType, Country.Ukraine, year)
    val monumentDb = Some(new MonumentDB(contest, Nil))
    val imageDb = new ImageDB(contest, Nil, monumentDb)
    ContestStat(
      contest = contest,
      startYear = year,
      monumentDb = monumentDb,
      currentYearImageDb = imageDb,
      totalImageDb = imageDb
    )
  }
}
