package org.scalawiki.wlx.stat.rating

import org.scalawiki.dto.Image
import org.scalawiki.wlx.stat.ContestStat

/** WLM UA 2026 rule 7.3.5: a monument photo gets a bonus depending on how many
  * interior photos of that monument were already known (on Commons or Wikipedia)
  * before the contest started:
  *   - 0 interior photos: 5 points
  *   - 1–5 interior photos: 3 points
  *   - 6 and more: 0 points
  *
  * The bonus is applied to every uploaded photo of the monument (as with the other
  * per-monument bonuses), not only to interior photos of the current year.
  */
case class NumberOfInteriorImagesBonus(
    stat: ContestStat,
    rateRanges: RateRanges
) extends Rater {

  val interiorImagesByMonument: Map[String, Int] = oldImages.toSeq
    .filter(NumberOfInteriorImagesBonus.isInterior)
    .groupBy(_.monumentId.getOrElse(""))
    .mapValues(_.size)
    .toMap

  override def rate(monumentId: String, author: String): Double =
    rateRanges.rate(interiorImagesByMonument.getOrElse(monumentId, 0))

  override def explain(monumentId: String, author: String): String = {
    val number = interiorImagesByMonument.getOrElse(monumentId, 0)
    val (rate, start, end) = rateRanges.rateWithRange(number)
    s"$number ($start-${end.getOrElse("")}) interior images existed, bonus = $rate"
  }
}

object NumberOfInteriorImagesBonus {

  /** An image is treated as an interior shot when it is tagged with an interior
    * special nomination (e.g. `WLM2026-UA-interior`) or filed in an "Interior of …"
    * category on Commons.
    */
  def isInterior(image: Image): Boolean =
    image.specialNominations.exists(_.toLowerCase.contains("interior")) ||
      image.categories.exists(_.toLowerCase.contains("interior"))
}
