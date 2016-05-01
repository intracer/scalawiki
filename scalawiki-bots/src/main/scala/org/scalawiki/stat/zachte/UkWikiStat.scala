package org.scalawiki.stat.zachte

import org.jsoup.Jsoup
import org.scalawiki.MwBot
import org.scalawiki.http.HttpClientSpray

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

object UkWikiStat {


  def main(args: Array[String]) {
    val http = new HttpClientSpray()
    val bot = MwBot.get(MwBot.commons)
    val lang = "UK"
    val site = "Wikipedia"
    val url  = s"https://stats.wikimedia.org/EN/Tables$site$lang.htm"

    http.get(url).foreach {
      html =>
        val doc = Jsoup.parse(html)
        val table = doc.select("table#table1").first()
            val rows = table.select("tr").asScala
            for(row <- rows) {
              val cols = row.select("td")
            }
//        val users = Seq("Otavioneto33", "Bevieira", "Nochita", "Fabiancm")
      //  users.foreach(message)
    }

        val user = "Ilya"
//    message(user)
  }

}
