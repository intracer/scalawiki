package org.scalawiki.wlx.stat.rating

import org.scalawiki.wlx.stat.ContestStat

case class NumberOfMonuments(stat: ContestStat, baseRate: Double) extends Rater {
  val monumentIds = stat.monumentDb.map(_.ids).getOrElse(Set.empty)

  override def rate(monumentId: String, author: String): Double = {
    if (monumentIds.contains(monumentId)) baseRate else 0
  }

  override def explain(monumentId: String, author: String): String = {
    if (monumentIds.contains(monumentId)) s"Base rate = $baseRate"
    else "Not a known monument = 0"
  }

  override def withRating: Boolean = false
}

case class NewlyPicturedBonus(stat: ContestStat, newlyPicturedRate: Double) extends Rater {

  override def rate(monumentId: String, author: String): Double = {
    if (!oldMonumentIds.contains(monumentId))
      newlyPicturedRate - 1
    else
      0
  }

  override def explain(monumentId: String, author: String): String = {
    if (!oldMonumentIds.contains(monumentId))
      s"Newly pictured rate bonus = ${newlyPicturedRate - 1}"
    else
      "Not newly pictured = 0"
  }
}
