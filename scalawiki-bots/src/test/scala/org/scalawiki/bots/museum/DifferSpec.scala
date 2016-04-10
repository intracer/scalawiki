package org.scalawiki.bots.museum

import org.specs2.mutable.Specification

case class Two(a: String, b: String)

class DifferSpec extends Specification {

  "differ" should {

    "not see changes" in {
      val t1 = Two("a1", "b1")
      val t2 = Two("a1", "b1")

      Differ.diff(t1, t2) === Map.empty
    }

    "see changed strings" in {
      val t1 = Two("a1", "b1")
      val t2 = Two("a1", "b2")

      Differ.diff(t1, t2) === Map(1 -> Diff("b", "b1", "b2"))
    }
  }
}