package org.scalawiki.wlx.actor

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import org.apache.pekko.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit.SerializationSettings
import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.Monument
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

class StatisticsActorSpec
    extends ScalaTestWithActorTestKit(
      EventSourcedBehaviorTestKit.config.withFallback(com.typesafe.config.ConfigFactory.load())
    )
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterEach {

  val contest = org.scalawiki.wlx.dto.Contest.WLMUkraine(2024)

  private val behaviorTestKit =
    EventSourcedBehaviorTestKit[Command, StatisticsEvent, StatisticsState](
      system,
      StatisticsActor(contest, persistenceId = "test-statistics-1"),
      SerializationSettings.disabled
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    behaviorTestKit.clear()
  }

  "StatisticsActor" should {

    "persist MonumentReceived event on DataFetched(MonumentReceived(...))" in {
      val monument = Monument(id = "01-001-0001", name = "Test")
      val result = behaviorTestKit.runCommand(DataFetched(MonumentReceived(monument)))

      result.event shouldBe MonumentReceived(monument)
      result.stateOfType[StatisticsState].monuments should contain(monument)
    }

    "persist MonumentsFetchCompleted and set monumentsDone = true" in {
      val result = behaviorTestKit.runCommand(DataFetched(MonumentsFetchCompleted))
      result.stateOfType[StatisticsState].monumentsDone shouldBe true
    }

    "persist ImageReceived and accumulate by year" in {
      val img = Image("File:Test.jpg")
      val result = behaviorTestKit.runCommand(DataFetched(ImageReceived(img, 2024)))
      result.stateOfType[StatisticsState].imagesByYear(2024) should contain(img)
    }

    "restore state after simulated restart" in {
      val m = Monument(id = "01-001-0001", name = "Test")
      behaviorTestKit.runCommand(DataFetched(MonumentReceived(m)))
      behaviorTestKit.runCommand(DataFetched(MonumentsFetchCompleted))

      behaviorTestKit.restart()

      val state = behaviorTestKit.getState()
      state.monuments should contain(m)
      state.monumentsDone shouldBe true
    }
  }
}
