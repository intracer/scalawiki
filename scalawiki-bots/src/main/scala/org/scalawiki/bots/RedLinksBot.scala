package org.scalawiki.bots

import org.jsoup.Jsoup
import org.scalawiki.http.HttpClient

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

object RedLinksBot {

  val linkPages = (1 to 7).map("User:Ahonc/WLM-2019_UA/images/" + _)

  val http = HttpClient.get()

  def main(args: Array[String]): Unit = {
    for (page <- linkPages) {
      val html = Await.result(http.get("https://commons.wikimedia.org/wiki/" + page), 1.minute)
      val doc = Jsoup.parse(html)
      val links = doc.select("a[href]").asScala
      val missingFiles = links.filter { link =>
        link.attr("class") == "new" && link.attr("title").startsWith("File")
      }.map(_.attr("title").replace(" (page does not exist)", ""))
      val text = s"\n== [[$page]] ==\n" + missingFiles.map(title => s"#[[$title]]").mkString("\n")
      println(text)
    }
  }
}