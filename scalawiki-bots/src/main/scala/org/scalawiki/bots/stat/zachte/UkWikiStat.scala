package org.scalawiki.bots.stat.zachte

import akka.actor.ActorSystem
import org.jsoup.Jsoup
import org.scalawiki.MwBot

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

object UkWikiStat {

  def main(args: Array[String]) {
    val system = ActorSystem()
    val http = new HttpClientAkka(system)
    val bot = MwBot.fromHost(MwBot.commons)
    val lang = "UK"
    val site = "Wikipedia"
    val url = s"https://stats.wikimedia.org/EN/Tables$site$lang.htm"

    http.get(url).foreach {
      html =>
        val doc = Jsoup.parse(html)
        val table = doc.select("table#table1").first()
        val rows = table.select("tr").asScala
        for (row <- rows) {
          val cols = row.select("td")
        }
    }
  }
}
