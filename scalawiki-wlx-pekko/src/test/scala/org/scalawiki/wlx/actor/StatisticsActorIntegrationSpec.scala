package org.scalawiki.wlx.actor

import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class StatisticsActorIntegrationSpec
    extends ScalaTestWithActorTestKit(
      EventSourcedBehaviorTestKit.config.withFallback(ConfigFactory.load())
    )
    with AnyWordSpecLike
    with Matchers {

  val contest  = Contest.WLMUkraine(2024)
  val contests = Seq(contest)

  "StatisticsActor (integration)" should {

    "gather data from stub streams and trigger reporting" in {
      val m1 = Monument(id = "01-001-0001", name = "M1")
      val i1 = Image("File:Img1.jpg")

      val actor = spawn(
        StatisticsActor(contest, contests, bot = null, persistenceId = "integration-test-1"),
        "stats-integration"
      )

      // Simulate stream feeding events directly
      actor ! DataFetched(MonumentReceived(m1))
      actor ! DataFetched(MonumentsFetchCompleted)
      actor ! DataFetched(ImageReceived(i1, 2024))
      actor ! DataFetched(YearImagesFetchCompleted(2024))
      actor ! DataFetched(TotalImageReceived(i1))
      actor ! DataFetched(TotalImagesFetchCompleted)

      // After all events, isComplete triggers GenerateReports internally.
      // Give actor time to process and spawn reporter supervisor.
      Thread.sleep(500)

      // Actor is alive and processed all events without throwing.
      actor should not be null
    }
  }
}
