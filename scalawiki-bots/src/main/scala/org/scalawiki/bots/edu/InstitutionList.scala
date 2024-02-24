package org.scalawiki.bots.edu

import java.net.URLEncoder

import org.jsoup.Jsoup
import org.scalawiki.http.HttpClient

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

case class Institution(
    id: Long,
    name: String,
    city: String,
    country: String,
    numCourses: Int,
    numStudents: Int
)

object InstitutionList {
  val http = HttpClient.get()

  val host = "uk.wikipedia.org"

  val specialPage = "Спеціальна:Інституції" // "Special:Institutions"

  val url =
    "https://" + host + "/wiki/" + URLEncoder.encode(specialPage, "UTF-8")

  def main(args: Array[String]): Unit = {

    http.get(url) map { html =>
      val doc = Jsoup.parse(html)

      val table = doc.select("table").get(0)

      val rows = table.select("tr").asScala
      for (row <- rows) {
        val cols = row.select("td").asScala

        if (cols.nonEmpty) {
          val institution = Institution(
            0,
            name = cols(0).text,
            city = cols(1).text,
            country = cols(2).text,
            numCourses = cols(3).text.toInt,
            numStudents = cols(4).text.toInt
          )

          println(institution)
        }
      }
    } recover { case t: Throwable =>
      println(t)
    }
  }
}
