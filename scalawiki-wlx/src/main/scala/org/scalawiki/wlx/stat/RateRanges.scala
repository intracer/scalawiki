package org.scalawiki.wlx.stat

import com.typesafe.config.Config

class RateRanges(val rangeMap: Map[(Int, Int), Int]) {
  verify()

  private def verify(): Unit = {
    rangeMap.keys.toBuffer.sorted.foreach { r1 =>
      rangeMap.keys.toBuffer.sorted.foreach { r2 =>
        if ((r1 ne r2) &&
          r1._1 <= r2._2 &&
          r2._1 <= r1._2) {
          throw new IllegalArgumentException(s"Ranges $r1 and $r2 overlap")
        }
      }
    }
  }

  def rate(param: Int): Option[Int] = {
    rangeMap.collectFirst { case ((start, end), rate) if start <= param && param <= end => rate }
  }
}

object RateRanges {

  import scala.collection.JavaConverters._

  def apply(config: Config): RateRanges = {
    val map = config.entrySet().asScala.map { entry =>
      val key = entry.getKey
      val rangeSeq = key.split("-").map(_.toInt).take(2)
      val rate = entry.getValue.unwrapped().asInstanceOf[Number].longValue().toInt
      ((rangeSeq.head, rangeSeq.last), rate)
    }.toMap
    new RateRanges(map)
  }
}