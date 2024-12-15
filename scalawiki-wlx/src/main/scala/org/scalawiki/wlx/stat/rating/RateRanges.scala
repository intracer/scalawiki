package org.scalawiki.wlx.stat.rating

import com.typesafe.config.Config

import scala.util.Try

case class RateRanges(
    rangeMap: Map[(Int, Int), Double],
    default: Int = 0,
    sameAuthorZeroBonus: Boolean = false
) {
  verify()
  val max = Try(rangeMap.keys.map(_._2).max).toOption.getOrElse(0)

  private def verify(): Unit = {
    rangeMap.keys.foreach { case (x1, x2) =>
      if (x1 > x2)
        throw new IllegalArgumentException(
          s"Invalid ends order in range $x1-$x2: $x1 > $x2"
        )
    }

    rangeMap.keys.toBuffer.sorted.foreach { r1 =>
      rangeMap.keys.toBuffer.sorted.foreach { r2 =>
        if (
          (r1 ne r2) &&
          r1._1 <= r2._2 &&
          r2._1 <= r1._2
        ) {
          throw new IllegalArgumentException(s"Ranges $r1 and $r2 overlap")
        }
      }
    }
  }

  def rate(param: Int): Double = {
    rangeMap
      .collectFirst {
        case ((start, end), rate) if start <= param && param <= end => rate
      }
      .getOrElse(0)
  }

  def rateWithRange(param: Int): (Double, Int, Option[Int]) = {
    rangeMap
      .collectFirst {
        case ((start, end), rate) if start <= param && param <= end =>
          (rate, start, Some(end))
      }
      .getOrElse((default, max, None))
  }

}

object RateRanges {

  import scala.collection.JavaConverters._

  def apply(config: Config): RateRanges = {
    val sameAuthorZeroBonus =
      Try(config.getBoolean("same-author-zero-bonus")).toOption.getOrElse(false)
    val map = config
      .entrySet()
      .asScala
      .filterNot(_.getKey == "same-author-zero-bonus")
      .map { entry =>
        val key = entry.getKey
        val rangeSeq = key.split("-").map(_.toInt).take(2)
        val rate = entry.getValue.unwrapped().asInstanceOf[Number].doubleValue()
        ((rangeSeq.head, rangeSeq.last), rate)
      }
      .toMap
    new RateRanges(map, sameAuthorZeroBonus = sameAuthorZeroBonus)
  }
}
