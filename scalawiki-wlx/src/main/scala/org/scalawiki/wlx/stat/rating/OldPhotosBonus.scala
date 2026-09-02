package org.scalawiki.wlx.stat.rating

import org.scalawiki.dto.Image
import org.scalawiki.wlx.stat.ContestStat

import java.time.{LocalDate, ZoneOffset, ZonedDateTime}

/** WLM UA 2026 rule 7.3.4: a monument photo gets an extra `bonus` points if every
  * photo of that monument already known before the contest started is dated earlier
  * than `beforeDate` (i.e. the monument has only "old" pictures).
  *
  * A photo's date is taken from EXIF metadata (DateTimeOriginal) when available,
  * otherwise from the upload/revision timestamp. If any known photo has no date at
  * all we cannot assert that "all photos are old", so no bonus is given.
  */
case class OldPhotosBonus(
    stat: ContestStat,
    bonus: Double,
    beforeDate: LocalDate
) extends Rater {

  private val cutoff: ZonedDateTime = beforeDate.atStartOfDay(ZoneOffset.UTC)

  private def imageDate(i: Image): Option[ZonedDateTime] =
    i.metadata.flatMap(_.date).orElse(i.date)

  val oldImagesByMonument: Map[String, Seq[Image]] =
    oldImages.toSeq.groupBy(_.monumentId.getOrElse(""))

  def qualifies(monumentId: String): Boolean = {
    val images = oldImagesByMonument.getOrElse(monumentId, Nil)
    images.nonEmpty && images.forall(i => imageDate(i).exists(_.isBefore(cutoff)))
  }

  override def rate(monumentId: String, author: String): Double =
    if (qualifies(monumentId)) bonus else 0

  override def explain(monumentId: String, author: String): String =
    if (qualifies(monumentId))
      s"All existing photos are dated before $beforeDate = $bonus"
    else
      s"Not all existing photos are dated before $beforeDate = 0"
}
