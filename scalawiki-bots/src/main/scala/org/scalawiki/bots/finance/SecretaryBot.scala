package org.scalawiki.bots.finance

import org.scalawiki.WithBot
import org.scalawiki.query.QueryLibrary

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SecretaryBot extends WithBot with QueryLibrary {

  val host = "ua.wikimedia.org"

  def fetchResolutions(): Future[Seq[Resolution]] = {
    for (all <- bot.run(pageLinksGenerator("Рішення Правління (список)"))) yield {
      all.flatMap(Resolution.fromPage).filter(r => r.year == 2016 || r.date == "28 грудня 2015").sorted
    }
  }

  def main(args: Array[String]): Unit = {
    fetchResolutions().map { res =>
      Resolution.saveHtml("resolutions.html", res)
    }
  }
}
