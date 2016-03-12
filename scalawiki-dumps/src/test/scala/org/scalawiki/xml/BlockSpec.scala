package org.scalawiki.xml

import org.specs2.mutable.Specification

class BlockSpec extends Specification {

  "block" should {
    "contain its start" in {
      Block(10, 4).contains(10) === true
    }
    "contain its end" in {
      Block(10, 4).contains(13) === true
    }
    "contain its range" in {
      val range = 10L to 13L
      range.size == 4
      range.forall(Block(10, 4).contains) === true
    }
    "not contain outside its range" in {
      (-10L to 9L).exists(Block(10, 4).contains) === false
      (14L to 20L).exists(Block(10, 4).contains) === false
    }
    "contain same block" in {
      Block(10, 4).contains(10, 4) === true
    }
    "contain smaller block" in {
      Block(10, 4).contains(10, 1) === true
      Block(10, 4).contains(11, 3) === true
      Block(10, 4).contains(10, 3) === true
      Block(10, 4).contains(11, 2) === true
      Block(10, 4).contains(13, 1) === true
    }
    "not contain overlapping block" in {
      Block(10, 4).contains(9, 2) === false
      Block(10, 4).contains(9, 4) === false
      Block(10, 4).contains(9, 5) === false
      Block(10, 4).contains(9, 6) === false
      Block(10, 4).contains(10, 5) === false
      Block(10, 4).contains(11, 4) === false
      Block(10, 4).contains(14, 2) === false
    }

    "not contain adjacent block" in {
      Block(10, 4).contains(0, 10) === false
      Block(10, 4).contains(14, 10) === false
      Block(10, 4).contains(14, 1) === false
    }

    "not contain not adjacent block" in {
      Block(10, 4).contains(0, 5) === false
      Block(10, 4).contains(20, 10) === false
    }
  }

}
