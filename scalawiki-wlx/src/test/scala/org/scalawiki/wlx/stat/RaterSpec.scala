package org.scalawiki.wlx.stat

import com.typesafe.config.ConfigFactory
import org.scalawiki.wlx.{ImageDB, MonumentDB}
import org.scalawiki.wlx.dto.{Contest, ContestType, Country}
import org.specs2.mutable.Specification

class RaterSpec extends Specification {

  "Rater" should {
    val contest = Contest(ContestType.WLM, Country.Ukraine, 2020)
    val monumentDb = Some(new MonumentDB(contest, Nil))
    val imageDb = new ImageDB(contest, Nil, monumentDb)
    val contestStat = ContestStat(
      contest = contest,
      startYear = 2019,
      monumentDb = monumentDb,
      currentYearImageDb = Some(imageDb),
      totalImageDb = Some(imageDb)
    )

    "parse wlm 2020" in {
      val rater =
        Rater.fromConfig(contestStat, ConfigFactory.load("wlm_ua.conf"))
      rater must beAnInstanceOf[RateSum]
      val rateSum = rater.asInstanceOf[RateSum]
      val raters = rateSum.raters
      raters.size === 3
      raters.find(_.isInstanceOf[NumberOfMonuments]) must beSome
      raters.find(_.isInstanceOf[NumberOfAuthorsBonus]) must beSome
      raters.find(_.isInstanceOf[NumberOfImagesInPlaceBonus]) must beSome

      val numberOfAuthorsBonus = raters.collect {
        case r: NumberOfAuthorsBonus => r
      }.head
      numberOfAuthorsBonus.rateRanges.sameAuthorZeroBonus === false

      val numberOfImagesInPlaceBonus = raters.collect {
        case r: NumberOfImagesInPlaceBonus => r
      }.head
      numberOfImagesInPlaceBonus.rateRanges.sameAuthorZeroBonus === false
    }

    "parse wle 2020" in {
      val rater =
        Rater.fromConfig(contestStat, ConfigFactory.load("wle_ua.conf"))
      rater must beAnInstanceOf[RateSum]
      val rateSum = rater.asInstanceOf[RateSum]
      val raters = rateSum.raters
      raters.size === 2
      raters.find(_.isInstanceOf[NumberOfMonuments]) must beSome
      raters.find(_.isInstanceOf[NumberOfAuthorsBonus]) must beSome
      val bonusRater = raters.collect { case x: NumberOfAuthorsBonus => x }.head
      bonusRater.rateRanges.rangeMap === Map(
        ((0, 0), 9),
        ((1, 3), 3),
        ((4, 9), 1)
      )
      bonusRater.rateRanges.sameAuthorZeroBonus === true
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
