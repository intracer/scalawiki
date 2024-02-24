package org.scalawiki.wlx.stat.generic

import org.specs2.mutable.Specification

class RecordsSpec extends Specification {

  "test" should {
    "sum numbers by tens" in {

      val numbers = 0 to 49
      val r = new Records[Int, Int](
        numbers,
        new Grouping[Int, Int]("tens", _ / 10),
        Seq(new Aggregation("sum", _.sum)),
        rowOrdering = new LongColumnOrdering(0)
      )

      r.rows === Seq(
        Seq(0, (0 + 9) * 5),
        Seq(1, (10 + 19) * 5),
        Seq(2, (20 + 29) * 5),
        Seq(3, (30 + 39) * 5),
        Seq(4, (40 + 49) * 5)
      )
    }

    "sum numbers by tens reversed order by sum" in {

      val numbers = 0 to 49
      val r = new Records[Int, Int](
        numbers,
        new Grouping[Int, Int]("tens", _ / 10),
        Seq(new Aggregation("sum", _.sum)),
        rowOrdering = new LongColumnOrdering(1, true)
      )

      r.rows === Seq(
        Seq(4, (40 + 49) * 5),
        Seq(3, (30 + 39) * 5),
        Seq(2, (20 + 29) * 5),
        Seq(1, (10 + 19) * 5),
        Seq(0, (0 + 9) * 5)
      )
    }

  }

}
