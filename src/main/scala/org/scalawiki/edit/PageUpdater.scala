package org.scalawiki.edit

import org.scalawiki.WithBot

class PageUpdater(task: PageUpdateTask) extends WithBot {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent._

  def host = task.host

  def update() = {
    val titles = task.titles
    val results = titles.zipWithIndex.map {
      case (title, index) =>
        println(s"Processing page: $title, $index of ${titles.size}")
        updatePage(title)
    }

    for (seq <- Future.sequence(results.toSeq)) {
      val (successful, errors) = seq.partition(_ == "Success")

      println(s"Successful page updates: ${successful.size}")
      println(s"Errors in  page updates: ${errors.size}")

      errors.foreach(println)
    }
  }

  def updatePage(title: String): Future[Any] = {
    bot.pageText(title).flatMap { pageText =>
      val (newText: String, comment: String) = task.updatePage(title, pageText)
      bot.page(title).edit(newText, Some(comment))
    }
  }

}
