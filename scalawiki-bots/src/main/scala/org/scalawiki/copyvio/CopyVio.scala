package org.scalawiki.copyvio

import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.prop._
import org.scalawiki.dto.cmd.query.{PageIdsParam, Query}
import org.scalawiki.http.HttpClient
import org.scalawiki.query.QueryLibrary
import org.scalawiki.{MwBot, WithBot}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

class CopyVio(val http: HttpClient) {

  implicit val sourceReads =
    ((__ \ "url").read[String] ~
      (__ \ "confidence").read[Double] ~
      (__ \ "violation").read[String] ~
      (__ \ "skipped").read[Boolean]) (CopyVioSource.apply _)

  val sourcesReads = (__ \ "sources").read[Seq[CopyVioSource]]

  def baseUrl(project: String = "wikipedia", lang: String = "uk") =
    s"https://tools.wmflabs.org/copyvios/api.json?version=1&action=search&project=$project&lang=$lang"

  def search(title: String, lang: String = "uk", project: String = "wikipedia") =
    http.get(baseUrl(project, lang) + s"&title=$title") map parseResponse

  def searchByRevId(revId: Long, lang: String = "uk", project: String = "wikipedia") =
    http.get(baseUrl(project, lang) + s"&oldid=$revId") map parseResponse

  def parseResponse(body: String) = Json.parse(body).validate(sourcesReads).get

}

object CopyVio extends WithBot with QueryLibrary {

  val host = MwBot.ukWiki

  def pagesByIds(ids: Seq[Long]): Future[Seq[Page]] = {
    import org.scalawiki.dto.cmd.query.prop.rvprop._

    val action = Action(Query(PageIdsParam(ids), Prop(Info(), Revisions(RvProp(Ids) /*,RvLimit("max")*/))))

    bot.run(action)
  }

  def main(args: Array[String]) {
    val copyVio = new CopyVio(HttpClient.get())

    val revIdsFuture = articlesWithTemplate("Вікіпедія любить пам'ятки")
    recover(revIdsFuture)

    val pagesF: Future[Seq[Page]] = revIdsFuture.flatMap[Seq[Page]] {
      ids =>
        val pagesFuture = pagesByIds(ids)
        recover(pagesFuture)

        pagesFuture
    }

    pagesF.foreach {

      pages =>
        println("pages: " + pages.size)

        pages.zipWithIndex.foreach {
          case (p, i) =>
            import scala.concurrent.duration._
            println(s"# [[${p.title}]]")

            val sourcesFuture: Future[Seq[CopyVioSource]] = copyVio.searchByRevId(p.revisions.head.revId.get)
            recover(sourcesFuture)
            val sources = Await.result(sourcesFuture, 30.minutes)

            val suspected = sources.filterNot(_.violation == "none") //s.violation == "suspected" || s.violation == "possible")

            if (suspected.nonEmpty) {
              for (s <- suspected) {
                println(s"## url: [${s.url}], violation ${s.violation}, confidence ${s.confidence}")
              }
            }
        }
    }
  }

  def recover(f: Future[_]) = {
    f.recover {
      case NonFatal(t) =>
        println("Error : " + t)
        throw t
    }
  }
}