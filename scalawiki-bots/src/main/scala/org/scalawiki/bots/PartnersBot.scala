package org.scalawiki.bots

import org.jsoup.Jsoup
import org.scalawiki.http.HttpClientSpray

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PartnersBot {

  val ourDomain = "wikilovesearth.org.ua"
  val partnersLinkPage = "http://" + ourDomain + "/partners/"

  val http = new HttpClientSpray()

  def main(args: Array[String]): Unit = {
    http.get(partnersLinkPage).foreach {
      html =>
        val doc = Jsoup.parse(html)
        val links = doc.select("a[href]").asScala
        val hrefs = links.map(_.attr("abs:href"))

        val external = hrefs.map(_.trim).filterNot(href => href.isEmpty || href.contains(ourDomain)).toSet

        Future.traverse(external) { link =>
          println("getting " + link)
          http.get(link).map { home =>
            val doc = Jsoup.parse(home)
            val title = doc.select("title").first().text()
            val elem = s"""  <li> <a href="$link">$title</a>"""
            println(elem)
            elem
          }
        }.foreach(println)
    }
  }
}
