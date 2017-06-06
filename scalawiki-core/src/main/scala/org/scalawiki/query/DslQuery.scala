package org.scalawiki.query

import org.scalawiki.MwBot
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.{MwException, Page}
import org.scalawiki.json.Parser

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class QueryProgress(pages: Long, done: Boolean, action: Action, bot: MwBot, context: Map[String, String] = Map.empty)

class DslQuery(val action: Action, val bot: MwBot, context: Map[String, String] = Map.empty) {

  import scala.concurrent.ExecutionContext.Implicits.global

  var startTime: Long = 0

  def run(continue: Map[String, String] = Map("continue" -> ""),
           pages: Seq[Page] = Seq.empty[Page],
           limit: Option[Long] = None): Future[Seq[Page]] = {

    val params = action.pairs ++ Seq("format" -> "json") ++ continue

    if (startTime == 0)
      startTime = System.nanoTime()

    onProgress(pages.size)

    bot.post(params.toMap) flatMap {
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
            bot.log.error(s"${bot.host} exception $withParams")
            Future.failed(withParams)
          case Failure(ex) =>
            bot.log.error(s"${bot.host} exception $ex")
            Future.failed(ex)
        }
    }
  }

  def mergePages(pages: Seq[Page], newPages: Seq[Page]): Seq[Page] = {
    val ids = pages.flatMap(_.id).toSet
    val newById = Page.groupById(newPages)

    val intersection = ids.intersect(newById.keySet)

    pages.map { p =>
      p.id.filter(intersection.contains).map { id =>
        p.appendLists(newById(id).head)
      }.getOrElse(p)
    } ++ newPages.filterNot { p => p.id.exists(intersection.contains) }
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
