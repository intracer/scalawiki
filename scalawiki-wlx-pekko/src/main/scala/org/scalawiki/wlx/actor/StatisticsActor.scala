package org.scalawiki.wlx.actor

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import org.scalawiki.wlx.dto.Contest

object StatisticsActor {

  def apply(
      contest: Contest,
      persistenceId: String = s"statistics-${System.currentTimeMillis()}"
  ): Behavior[Command] =
    EventSourcedBehavior[Command, StatisticsEvent, StatisticsState](
      persistenceId  = PersistenceId.ofUniqueId(persistenceId),
      emptyState     = StatisticsState(),
      commandHandler = commandHandler(contest),
      eventHandler   = (state, event) => state.applyEvent(event)
    ).snapshotWhen { (_, event, _) =>
      event == MonumentsFetchCompleted ||
      event.isInstanceOf[YearImagesFetchCompleted] ||
      event == TotalImagesFetchCompleted
    }.withRetention(
      RetentionCriteria.snapshotEvery(numberOfEvents = 500, keepNSnapshots = 2)
    )

  private def commandHandler(
      contest: Contest
  ): (StatisticsState, Command) => Effect[StatisticsEvent, StatisticsState] = {
    (state, command) =>
      command match {
        case DataFetched(event) =>
          Effect.persist(event)

        case GatherData(_, replyTo) =>
          // TODO Task 9: start DataFetchingStream
          Effect.none.thenRun(_ => replyTo ! StatusReply.success(Done))

        case GenerateReports =>
          // TODO Task 9: spawn ReporterSupervisorActor
          Effect.none

        case AllReportsDoneInternal =>
          Effect.none
      }
  }
}
