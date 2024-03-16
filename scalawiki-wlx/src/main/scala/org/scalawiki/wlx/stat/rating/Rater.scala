package org.scalawiki.wlx.stat.rating

import com.typesafe.config.Config
import org.scalawiki.wlx.ImageDB
import org.scalawiki.wlx.stat.ContestStat

import scala.util.Try


trait Rater {

  def stat: ContestStat

  def imageDb: ImageDB = stat.currentYearImageDb.get

  def rate(monumentId: String, author: String): Double

  def explain(monumentId: String, author: String): String

  def disqualify(monumentId: String, author: String): Boolean = false

  def rateMonumentIds(monumentIds: Set[String], author: String): Double = {
    monumentIds.toSeq.map(rate(_, author)).sum
  }

  def rateRegion(regionId: String, author: String): Double = {
    rateMonumentIds(
      imageDb._byAuthorAndRegion
        .by(author, regionId)
        .flatMap(_.monumentId)
        .toSet,
      author
    )
  }

  val oldImages = stat.oldImages

  lazy val oldMonumentIds: Set[String] = oldImages.flatMap(_.monumentId).toSet

  def withRating: Boolean = true

}

object Rater {

  def create(stat: ContestStat): Rater = {
    stat.contest.config.map(fromConfig(stat, _)).getOrElse {
      val config = stat.contest.rateConfig

      val raters = Seq(NumberOfMonuments(stat, config.baseRate)) ++
        config.newAuthorObjectRating
          .map(r =>
            NewlyPicturedPerAuthorBonus(
              stat,
              config.newObjectRating.getOrElse(1),
              r
            )
          )
          .orElse(
            config.newObjectRating.map(NewlyPicturedBonus(stat, _))
          )

      if (raters.tail.isEmpty) {
        raters.head
      } else {
        RateSum(stat, raters)
      }
    }
  }

  def fromConfig(stat: ContestStat, config: Config): Rater = {
    val rateCfg = config.getConfig(s"rates.${stat.contest.year}")
    val raters = Seq(
      NumberOfMonuments(
        stat,
        Try(rateCfg.getDouble("base-rate")).toOption.getOrElse(1)
      )
    ) ++
      (if (rateCfg.hasPath("number-of-authors-bonus")) {
         Seq(
           NumberOfAuthorsBonus(
             stat,
             RateRanges(rateCfg.getConfig("number-of-authors-bonus"))
           )
         )
       } else Nil) ++
      (if (rateCfg.hasPath("number-of-images-bonus")) {
         Seq(
           NumberOfImagesInPlaceBonus(
             stat,
             RateRanges(rateCfg.getConfig("number-of-images-bonus"))
           )
         )
       } else Nil)

    if (raters.tail.isEmpty) {
      raters.head
    } else {
      RateSum(stat, raters)
    }

  }
}