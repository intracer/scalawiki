package org.scalawiki.wlx.actor

import org.apache.pekko.Done
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.Monument
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

class DataFetchingStreamSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll {

  implicit val patience: PatienceConfig = PatienceConfig(timeout = Span(10, Seconds))

  val testKit: ActorTestKit = ActorTestKit(com.typesafe.config.ConfigFactory.load())
  implicit val system: ActorSystem[Nothing] = testKit.system

  override def afterAll(): Unit = {
    testKit.shutdownTestKit()
    super.afterAll()
  }

  val m1: Monument = Monument(id = "01-001-0001", name = "M1")
  val m2: Monument = Monument(id = "01-001-0002", name = "M2")
  val img: Image   = Image("File:Test.jpg")

  "DataFetchingStream" should {

    "send MonumentReceived per monument then MonumentsFetchCompleted" in {
      val probe = testKit.createTestProbe[Command]()

      DataFetchingStream.runWithSources(
        monumentSource = Source(Seq(m1, m2)),
        yearSources    = Map.empty,
        totalSource    = Source.empty,
        total          = false,
        target         = probe.ref,
        state          = StatisticsState()
      ).futureValue

      probe.expectMessage(DataFetched(MonumentReceived(m1)))
      probe.expectMessage(DataFetched(MonumentReceived(m2)))
      probe.expectMessage(DataFetched(MonumentsFetchCompleted))
    }

    "skip monument stream if monumentsDone is true" in {
      val probe = testKit.createTestProbe[Command]()

      DataFetchingStream.runWithSources(
        monumentSource = Source(Seq(m1)),
        yearSources    = Map.empty,
        totalSource    = Source.empty,
        total          = false,
        target         = probe.ref,
        state          = StatisticsState(monumentsDone = true)
      ).futureValue

      probe.expectNoMessage()
    }

    "send ImageReceived per image then YearImagesFetchCompleted" in {
      val probe = testKit.createTestProbe[Command]()

      DataFetchingStream.runWithSources(
        monumentSource = Source.empty,
        yearSources    = Map(2024 -> Source(Seq(img))),
        totalSource    = Source.empty,
        total          = false,
        target         = probe.ref,
        state          = StatisticsState(monumentsDone = true)
      ).futureValue

      probe.expectMessage(DataFetched(ImageReceived(img, 2024)))
      probe.expectMessage(DataFetched(YearImagesFetchCompleted(2024)))
    }

    "skip year stream for years already in yearsComplete" in {
      val probe = testKit.createTestProbe[Command]()

      DataFetchingStream.runWithSources(
        monumentSource = Source.empty,
        yearSources    = Map(2024 -> Source(Seq(img))),
        totalSource    = Source.empty,
        total          = false,
        target         = probe.ref,
        state          = StatisticsState(monumentsDone = true, yearsComplete = Set(2024))
      ).futureValue

      probe.expectNoMessage()
    }

    "send TotalImageReceived per image then TotalImagesFetchCompleted when total=true" in {
      val probe = testKit.createTestProbe[Command]()

      DataFetchingStream.runWithSources(
        monumentSource = Source.empty,
        yearSources    = Map.empty,
        totalSource    = Source(Seq(img)),
        total          = true,
        target         = probe.ref,
        state          = StatisticsState(monumentsDone = true, totalDone = false)
      ).futureValue

      probe.expectMessage(DataFetched(TotalImageReceived(img)))
      probe.expectMessage(DataFetched(TotalImagesFetchCompleted))
    }
  }
}
