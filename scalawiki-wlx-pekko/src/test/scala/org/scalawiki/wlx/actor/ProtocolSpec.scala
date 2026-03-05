package org.scalawiki.wlx.actor

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ProtocolSpec extends AnyWordSpec with Matchers {

  "StatisticsState" should {

    "not be complete when empty" in {
      val state = StatisticsState()
      state.isComplete(Set(2024, 2025)) shouldBe false
    }

    "not be complete when only monuments done" in {
      val state = StatisticsState(monumentsDone = true)
      state.isComplete(Set(2024)) shouldBe false
    }

    "be complete when all phases done" in {
      val state = StatisticsState(
        monumentsDone = true,
        yearsComplete = Set(2024),
        totalDone = true
      )
      state.isComplete(Set(2024)) shouldBe true
    }

    "not be complete when a year is missing" in {
      val state = StatisticsState(
        monumentsDone = true,
        yearsComplete = Set(2023),
        totalDone = true
      )
      state.isComplete(Set(2023, 2024)) shouldBe false
    }
  }
}
