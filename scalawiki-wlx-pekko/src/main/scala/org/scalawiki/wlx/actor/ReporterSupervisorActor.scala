package org.scalawiki.wlx.actor

import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.scalawiki.wlx.stat.ContestStat

import scala.concurrent.Future
import scala.util.{Failure, Success}

/** A named reporter: runs one report and returns Future[Unit]. */
case class NamedReporter(name: String, run: ContestStat => Future[Unit])

object ReporterSupervisorActor {

  sealed trait Response
  case object AllReportsDone extends Response

  private[ReporterSupervisorActor] sealed trait Msg
  private[ReporterSupervisorActor] case class Done(name: String)                  extends Msg
  private[ReporterSupervisorActor] case class Failed(name: String, ex: Throwable) extends Msg

  def apply(
      reporters: List[NamedReporter],
      stat: ContestStat,
      replyTo: ActorRef[Response]
  ): Behavior[Msg] =
    Behaviors.setup { context =>
      if (reporters.isEmpty) {
        replyTo ! AllReportsDone
        Behaviors.stopped
      } else {
        reporters.foreach { reporter =>
          context.pipeToSelf(reporter.run(stat)) {
            case Success(_) => Done(reporter.name)
            case Failure(e) => Failed(reporter.name, e)
          }
        }
        waiting(context, replyTo, pending = reporters.map(_.name).toSet)
      }
    }

  private def waiting(
      context: ActorContext[Msg],
      replyTo: ActorRef[Response],
      pending: Set[String]
  ): Behavior[Msg] =
    Behaviors.receiveMessage {
      case Done(name) =>
        val remaining = pending - name
        if (remaining.isEmpty) { replyTo ! AllReportsDone; Behaviors.stopped }
        else waiting(context, replyTo, remaining)

      case Failed(name, ex) =>
        context.log.error(s"Reporter '$name' failed: ${ex.getMessage}", ex)
        val remaining = pending - name
        if (remaining.isEmpty) { replyTo ! AllReportsDone; Behaviors.stopped }
        else waiting(context, replyTo, remaining)
    }
}
