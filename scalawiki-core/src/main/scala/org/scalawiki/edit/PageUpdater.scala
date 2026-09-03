package org.scalawiki.edit

import org.scalawiki.WithBot

import scala.concurrent._
import scala.util.{Failure, Success, Try}

class PageUpdater(task: PageUpdateTask) extends WithBot {

  import scala.concurrent.ExecutionContext.Implicits.global

  def host = task.host

  /** Update every page from [[PageUpdateTask.titles]], one at a time.
    *
    * The pages are processed serially on purpose:
    *   - [[https://www.mediawiki.org/wiki/API:Etiquette API:Etiquette]] asks bots
    *     to make requests one after another, not in parallel;
    *   - the host connection pool is deliberately tiny
    *     (`pekko.http.host-connection-pool.max-connections = 2`).
    *
    * Firing an edit future per page at once (as this did previously) submits far
    * more than `max-open-requests` (32) concurrent requests to that pool, so all
    * but the first few fail straight away with a `BufferOverflowException` and the
    * run stops after a handful of edits. Chaining the pages keeps at most one
    * request in flight and lets the whole batch finish.
    *
    * A failure on one page is logged and skipped, not propagated, so one bad page
    * cannot abort the rest of the batch.
    *
    * @return the per-page outcome, in processing order
    */
  def update(): Future[Seq[(String, Try[Any])]] = {
    val titles = task.titles.toVector
    val total = titles.size

    val done = titles.zipWithIndex.foldLeft(
      Future.successful(Vector.empty[(String, Try[Any])])
    ) { case (acc, (title, index)) =>
      acc.flatMap { results =>
        println(s"Processing page: $title, ${index + 1} of $total")
        updatePage(title)
          .map(Success(_): Try[Any])
          .recover { case e => Failure(e) }
          .map(outcome => results :+ (title -> outcome))
      }
    }

    done.foreach { results =>
      val (successful, errors) = results.partition(_._2.isSuccess)
      println(s"Successful page updates: ${successful.size}")
      println(s"Errors in  page updates: ${errors.size}")
      errors.foreach { case (title, outcome) => println(s"  $title: $outcome") }
    }

    done
  }

  def updatePage(title: String): Future[Any] = {
    bot.pageText(title).flatMap { pageText =>
      val (newText: String, comment: String) = task.updatePage(title, pageText)
      bot.page(title).edit(newText, Some(comment))
    }
  }

}
