package org.scalawiki.wlx.actor

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import org.scalawiki.MwBot
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.stat.StatConfig

object StatisticsActor {

  def apply(
      contest: Contest,
      contests: Seq[Contest] = Nil,
      bot: MwBot = null,
      cfg: StatConfig = StatConfig(campaign = ""),
      persistenceId: String = s"statistics-${System.currentTimeMillis()}"
  ): Behavior[Command] =
    Behaviors.setup { context =>
      val expectedYears = contests.map(_.year).toSet

      EventSourcedBehavior[Command, StatisticsEvent, StatisticsState](
        persistenceId  = PersistenceId.ofUniqueId(persistenceId),
        emptyState     = StatisticsState(),
        commandHandler = commandHandler(contest, contests, expectedYears, bot, cfg, context),
        eventHandler   = (state, event) => state.applyEvent(event)
      ).snapshotWhen { (_, event, _) =>
        event == MonumentsFetchCompleted ||
        event.isInstanceOf[YearImagesFetchCompleted] ||
        event == TotalImagesFetchCompleted
      }.withRetention(
        RetentionCriteria.snapshotEvery(numberOfEvents = 500, keepNSnapshots = 2)
      )
    }

  private def commandHandler(
      contest: Contest,
      contests: Seq[Contest],
      expectedYears: Set[Int],
      bot: MwBot,
      cfg: StatConfig,
      context: org.apache.pekko.actor.typed.scaladsl.ActorContext[Command]
  ): (StatisticsState, Command) => Effect[StatisticsEvent, StatisticsState] = {
    (state, command) =>
      command match {

        case DataFetched(event) =>
          Effect.persist(event).thenRun { newState: StatisticsState =>
            if (newState.isComplete(expectedYears)) {
              context.self ! GenerateReports
            }
          }

        case GatherData(total, replyTo) =>
          Effect.none.thenRun { currentState: StatisticsState =>
            if (!currentState.isComplete(expectedYears) && bot != null) {
              import org.apache.pekko.actor.typed.scaladsl.adapter._
              DataFetchingStream.run(
                contest  = contest,
                contests = contests,
                bot      = bot,
                total    = total,
                target   = context.self,
                state    = currentState
              )(context.system)
            }
            replyTo ! StatusReply.success(Done)
          }

        case GenerateReports =>
          Effect.none.thenRun { currentState: StatisticsState =>
            val startYear = contests.headOption.map(_.year).getOrElse(contest.year)
            val stat = currentState.toContestStat(contest, startYear, cfg)
            // Use an empty reporter list for now — Task 10 adds the real reporters
            val reporters: List[NamedReporter] = Nil
            val replyAdapter: ActorRef[ReporterSupervisorActor.Response] =
              context.messageAdapter {
                case ReporterSupervisorActor.AllReportsDone => AllReportsDoneInternal
              }
            context.spawn(
              ReporterSupervisorActor(reporters, stat, replyAdapter),
              s"reporter-supervisor-${System.currentTimeMillis()}"
            )
          }

        case AllReportsDoneInternal =>
          Effect.none.thenRun(_ => context.log.info("All reports done"))
      }
  }
}
