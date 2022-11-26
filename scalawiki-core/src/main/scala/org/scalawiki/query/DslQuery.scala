package org.scalawiki.query

import org.scalawiki.MwBot
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.{MwException, Page}
import org.scalawiki.json.Parser

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success}

case class QueryProgress(pages: Long, done: Boolean, action: Action, bot: MwBot, context: Map[String, String] = Map.empty)

class DslQuery(val action: Action, val bot: MwBot, context: Map[String, String] = Map.empty) {

  import scala.concurrent.ExecutionContext.Implicits.global

  var startTime: Long = 0

  def run(continue: Map[String, String] = Map("continue" -> ""),
          pages: mutable.LinkedHashMap[Long, Page] = mutable.LinkedHashMap.empty,
          limit: Option[Long] = None): Future[mutable.LinkedHashMap[Long, Page]] = {

    val params = action.pairs ++ Seq("format" -> "json", "utf8" -> "") ++ continue

    if (startTime == 0)
      startTime = System.nanoTime()

    onProgress(pages.size)

    implicit val success: retry.Success[String] = retry.Success[String](_ => true)

    // TODO fix memory leak
    retry.Backoff()(odelay.Timer.default)(() => bot.post(params.toMap)) flatMap {
      body =>
        val parser = new Parser(action)

        parser.parse(body) match {
          case Success(newPages) =>
            val allPages = mergePages(pages, newPages)

            val newContinue = parser.continue
            if (newContinue.isEmpty || limit.exists(_ <= allPages.size)) {

              onProgress(allPages.size, done = true)
              Future.successful(allPages)
            } else {
              run(newContinue, allPages, limit)
            }

          case Failure(mwEx: MwException) =>
            val withParams = mwEx.copy(params = params.toMap)
            bot.log.error(s"${bot.host} exception $withParams, body: $body")
            Future.failed(withParams)
          case Failure(ex) =>
            bot.log.error(s"${bot.host} exception $ex, body: $body")
            Future.failed(ex)
        }
    }
  }

  def mergePages(pages: mutable.LinkedHashMap[Long, Page], newPages: Seq[Page]): mutable.LinkedHashMap[Long, Page] = {
    for (page <- newPages;
         id <- page.id) {
      pages(id) = pages.get(id)
        .map(_.appendLists(page))
        .getOrElse(page)
    }
    pages
  }

  def onProgress(pages: Long, done: Boolean = false) = {
    if (done) {
      val estimatedTime = (System.nanoTime() - startTime) / Math.pow(10, 9)

      bot.log.info(s"${bot.host} Action completed with $pages pages in $estimatedTime seconds,  $action.pairs")
    } else {
      bot.log.info(s"${bot.host} pages: $pages action: $action.pairs")
    }

    val progress = new QueryProgress(pages, done, action, bot, context)

    bot.system.eventStream.publish(progress)
  }
}
