package org.scalawiki.wlx.actor

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.scalawiki.MwBot
import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.Action
import org.scalawiki.json.Parser

import scala.concurrent.Future
import scala.util.{Failure, Success}

object MwSource {

  /**
   * Returns a Source that emits one Seq[Page] per MediaWiki API continuation batch.
   * Uses Source.unfoldAsync over the pagination continuation mechanism.
   * Terminates when the API returns no further continuation token.
   */
  def pages(action: Action, bot: MwBot)(implicit
      system: ActorSystem
  ): Source[Seq[Page], NotUsed] = {

    import system.dispatcher

    type State = Option[Map[String, String]]
    val initial: State = Some(Map("continue" -> ""))

    Source.unfoldAsync[State, Seq[Page]](initial) {
      case None =>
        Future.successful(None) // stream exhausted

      case Some(continueMap) =>
        val params = action.pairs.toMap ++
          Map("format" -> "json", "utf8" -> "") ++
          continueMap

        bot.post(params).map { body =>
          val parser = new Parser(action)
          parser.parse(body) match {
            case Success(newPages) =>
              val nextState: State =
                if (parser.continue.isEmpty) None
                else Some(parser.continue)
              Some((nextState, newPages))

            case Failure(ex) =>
              throw ex
          }
        }
    }
  }
}
