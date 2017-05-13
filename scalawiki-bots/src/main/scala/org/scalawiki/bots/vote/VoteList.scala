package org.scalawiki.bots.vote

import org.jsoup.Jsoup
import org.scalawiki.http.HttpClientAkka

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

object VoteList {

  val http = new HttpClientAkka()

  val host = "vote.wikimedia.org"

  val specialPage = "Special:SecurePoll/list/637"

  val url = "https://vote.wikimedia.org/w/index.php?title=Special:SecurePoll/list/637&limit=5000&sort=vote_voter_name"

  def main(args: Array[String]): Unit = {

    http.get(url) map { html =>
      val doc = Jsoup.parse(html)

      val table = doc.select("table").get(0)

      val rows = table.select("tr").asScala
      val names = for (row <- rows) yield {
        val cols = row.select("td").asScala

          if (cols.nonEmpty) {
            val name = cols(1).text
            val wiki = cols(2).text
            Some(s"'$name'")
              .filter(_ => wiki == "uk.wikipedia.org")
          } else None
      }

      println(names.flatten.distinct.mkString(",\n"))

    } recover {
      case t: Throwable => println(t)
    }
  }

}
