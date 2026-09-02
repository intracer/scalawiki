package org.scalawiki.wlx

import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.stat.ContestStat
import org.scalawiki.wlx.stat.rating.Rater

import scala.util.Try

/** Fills the "rating" field of WLM/WLE monument list rows with the number of points
  * a new photo of that monument would score, per the contest's rating rules
  * (`Rater`). WLM UA 2026 lists gained a `бали` field for this; the list template
  * renders it as a "(+ N балів)" hint next to the upload link.
  *
  * Meant to be run before the contest with a multi-year range (e.g.
  * `--start-year 2013 --year 2026`) so the bonuses are computed against photos that
  * already existed.
  */
object RatingListFiller {

  /** Template parameter that holds the rating, overridable via the `ratingListParam`
    * key in the campaign config (`wlm_ua.conf` etc.). */
  val DefaultParam = "бали"

  def paramName(stat: ContestStat): String =
    stat.contest.config
      .flatMap(c => Try(c.getString("ratingListParam")).toOption)
      .getOrElse(DefaultParam)

  def fillLists(stat: ContestStat): Unit = {
    val monumentDb = stat.monumentDb.getOrElse {
      throw new IllegalStateException("RatingListFiller needs a monument database")
    }
    val updater = new RatingUpdater(monumentDb, Rater.create(stat), paramName(stat))
    ListUpdater.updateLists(monumentDb, updater)
  }
}

class RatingUpdater(
    monumentDb: MonumentDB,
    rater: Rater,
    paramName: String = RatingListFiller.DefaultParam
) extends MonumentUpdater {

  private def ratingValue(monument: Monument): Option[String] =
    if (!monumentDb.ids.contains(monument.id)) None
    else {
      val rate = rater.rate(monument.id, "")
      if (rate > 0) Some(RatingUpdater.format(rate)) else None
    }

  override def updatedParams(monument: Monument): Map[String, String] =
    ratingValue(monument).map(paramName -> _).toMap

  override def needsUpdate(monument: Monument): Boolean =
    ratingValue(monument).exists { value =>
      !monument.otherParams.get(paramName).map(_.trim).contains(value)
    }
}

object RatingUpdater {

  /** Whole numbers are written without a trailing ".0" so the wiki value stays
    * `12` rather than `12.0`. */
  def format(rate: Double): String =
    if (rate == Math.rint(rate) && !rate.isInfinite) rate.toLong.toString
    else rate.toString
}
