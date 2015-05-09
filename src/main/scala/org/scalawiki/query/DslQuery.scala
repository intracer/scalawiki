package org.scalawiki.query

import org.scalawiki.dto.{MwException, Page}
import org.scalawiki.dto.cmd.Action
import org.scalawiki.json.Parser
import org.scalawiki.MwBot

import scala.concurrent.Future
import scala.util.{Failure, Success}

class DslQuery(action: Action, site: MwBot) {

  import site.system.dispatcher

  def run(
           continue: Map[String, String] = Map("continue" -> ""),
           pages: Seq[Page] = Seq.empty[Page]
           ): Future[Seq[Page]] = {

    val params = action.pairs ++ Seq("format" -> "json", "bot" -> "x") ++ continue

    site.get(params.toMap) flatMap {
      body =>
        val parser = new Parser(action)

        parser.parse(body) match {
          case Success(newPages) =>
            val allPages = pages ++ newPages

            val newContinue = parser.continue
            if (newContinue.isEmpty) {
              Future.successful(allPages)
            } else {
              run(newContinue, allPages)
            }

          case Failure(mwEx: MwException) =>
            Future.failed(mwEx.copy(params = params.toMap))
          case Failure(ex) =>
            Future.failed(ex)
        }
    }
  }
}
