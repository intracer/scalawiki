package org.scalawiki.query

import org.scalawiki.dto.{MwException, Page}
import org.scalawiki.dto.cmd.Action
import org.scalawiki.json.Parser
import org.scalawiki.MwBot

import scala.concurrent.Future
import scala.util.{Failure, Success}

class DslQuery(val action: Action, val bot: MwBot) {

  import bot.system.dispatcher

  var startTime: Long = 0

  def run(
           continue: Map[String, String] = Map("continue" -> ""),
           pages: Seq[Page] = Seq.empty[Page]
           ): Future[Seq[Page]] = {

    val params = action.pairs ++ Seq("format" -> "json", "bot" -> "x") ++ continue

    bot.log.info(s"${bot.host} pages: ${pages.size} action: $params")
    if (startTime == 0)
      startTime = System.nanoTime()

    bot.get(params.toMap) flatMap {
      body =>
        val parser = new Parser(action)

        parser.parse(body) match {
          case Success(newPages) =>
            val allPages = pages ++ newPages

            val newContinue = parser.continue
            if (newContinue.isEmpty) {
              val estimatedTime = (System.nanoTime() - startTime) / Math.pow(10, 9)

              bot.log.info(s"${bot.host} Action completed with ${allPages.size} pages in $estimatedTime seconds,  $params")
              Future.successful(allPages)
            } else {
              run(newContinue, allPages)
            }

          case Failure(mwEx: MwException) =>
            val withParams = mwEx.copy(params = params.toMap)
            bot.log.error(s"${bot.host} exception $withParams")
            Future.failed(withParams)
          case Failure(ex) =>
            bot.log.error(s"${bot.host} exception $ex")
            Future.failed(ex)
        }
    }
  }
}
