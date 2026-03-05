package org.scalawiki.wlx.actor

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future

class ReporterSupervisorActorSpec
    extends ScalaTestWithActorTestKit(com.typesafe.config.ConfigFactory.load())
    with AnyWordSpecLike
    with Matchers {

  val contest = org.scalawiki.wlx.dto.Contest.WLMUkraine(2024)
  val cfg     = org.scalawiki.wlx.stat.StatConfig(campaign = "wlm-ua", years = Seq(2024))
  val state   = StatisticsState(
    monumentsDone = true,
    yearsComplete = Set(2024),
    totalDone     = true
  )
  val stat = state.toContestStat(contest, 2024, cfg)

  "ReporterSupervisorActor" should {

    "send AllReportsDone when all reporters succeed" in {
      val replyProbe = createTestProbe[ReporterSupervisorActor.Response]()

      val reporters = List(
        NamedReporter("r1", _ => Future.successful(())),
        NamedReporter("r2", _ => Future.successful(()))
      )

      spawn(ReporterSupervisorActor(reporters, stat, replyProbe.ref))

      replyProbe.expectMessage(ReporterSupervisorActor.AllReportsDone)
    }

    "send AllReportsDone even when one reporter fails" in {
      val replyProbe = createTestProbe[ReporterSupervisorActor.Response]()

      val reporters = List(
        NamedReporter("ok",   _ => Future.successful(())),
        NamedReporter("fail", _ => Future.failed(new RuntimeException("boom")))
      )

      spawn(ReporterSupervisorActor(reporters, stat, replyProbe.ref))

      replyProbe.expectMessage(ReporterSupervisorActor.AllReportsDone)
    }

    "send AllReportsDone immediately with empty reporter list" in {
      val replyProbe = createTestProbe[ReporterSupervisorActor.Response]()

      spawn(ReporterSupervisorActor(List.empty, stat, replyProbe.ref))

      replyProbe.expectMessage(ReporterSupervisorActor.AllReportsDone)
    }
  }
}
