package org.scalawiki.wlx.stat

import org.scalawiki.dto.markup.Table

class RateInputDistribution(val stat: ContestStat,
                            val distribution: Map[Int, Int],
                            val name: String,
                           val headers: Seq[String]) extends Reporter {

  override def table: Table = {
    Table(headers, distribution.toSeq.sortBy(_._1).map{
      case (x1, x2) => Seq(x1, x2).map(_.toString)
    })
  }

}