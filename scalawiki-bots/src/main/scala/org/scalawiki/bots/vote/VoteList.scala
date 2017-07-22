package org.scalawiki.bots.vote

import org.jsoup.Jsoup
import org.scalawiki.http.HttpClient

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object VoteList {

  val http = HttpClient.get()

  val host = "vote.wikimedia.org"

  val specialPage = "Special:SecurePoll/list/637"

  val url = "https://vote.wikimedia.org/w/index.php?title=Special:SecurePoll/list/637&limit=5000&sort=vote_voter_name"

  def main(args: Array[String]): Unit = {

    for (names <- votedUsers) {
      println(names.flatten.distinct.mkString(",\n"))
    }
  }

  def votedUsers: Future[Seq[String]] = {
    val names = http.get(url) map { html =>
      val doc = Jsoup.parse(html)

      val table = doc.select("table").get(0)

      val rows = table.select("tr").asScala
      val names = for (row <- rows) yield {
        val cols = row.select("td").asScala

        if (cols.nonEmpty) {
          val name = cols(1).text
          val wiki = cols(2).text
          Some(s"'$name'")
            .filter(_ => wiki == "es.wikipedia.org")
        } else None
      }

      names.flatten.distinct

    } recover {
      case t: Throwable => println(t)
        Seq.empty
    }
    names
  }
}