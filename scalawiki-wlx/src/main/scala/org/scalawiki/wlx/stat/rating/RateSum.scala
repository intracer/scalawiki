package org.scalawiki.wlx.stat.rating

import org.scalawiki.wlx.stat.ContestStat

case class RateSum(stat: ContestStat, raters: Seq[Rater]) extends Rater {
  override def rate(monumentId: String, author: String): Double = {
    if (!raters.exists(_.disqualify(monumentId, author))) {
      raters.map(_.rate(monumentId, author)).sum
    } else 0
  }

  override def explain(monumentId: String, author: String): String = {
    val disqualifiedRater = raters.find(_.disqualify(monumentId, author))
    disqualifiedRater.fold(
      s"Rating = ${raters.map(_.rate(monumentId, author)).sum}, is a sum of: " + raters
        .map(_.explain(monumentId, author))
        .mkString(", ")
    ) { rater =>
      rater.explain(monumentId, author)
    }
  }
}
