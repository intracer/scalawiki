package org.scalawiki.copyvio

import akka.actor.ActorSystem
import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.list.{EiLimit, EiTitle, EmbeddedIn}
import org.scalawiki.dto.cmd.query.prop._
import org.scalawiki.dto.cmd.query.{Generator, PageIdsParam, Query}
import org.scalawiki.http.{HttpClient, HttpClientImpl}
import org.scalawiki.query.DslQuery
import org.scalawiki.{MwBot, WithBot}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

class CopyVio(val http: HttpClient) {

  def sourcesReads: Reads[Seq[CopyVioSource]] = {

    implicit val sourceReads: Reads[CopyVioSource] = (
      (__ \ "url").read[String] and
        (__ \ "confidence").read[Double] and
        (__ \ "violation").read[String] and
        (__ \ "skipped").read[Boolean]
      )(CopyVioSource.apply _)

    (__ \ "sources").read[Seq[CopyVioSource]]
  }

  val baseUrl = "https://tools.wmflabs.org/copyvios/api.json?version=1&action=search&"

  def search(title: String, lang: String = "uk", project: String = "wikipedia") = {
    val url = s"project=$project&lang=$lang&title=$title"
    val sources = http.get(baseUrl + url) map parseResponse
    sources
  }

  def searchByRevId(revId: Long, lang: String = "uk", project: String = "wikipedia") = {

    val url = s"project=$project&lang=$lang&oldid=$revId"

    http.get(baseUrl + url) map parseResponse
  }

  def parseResponse(body: String): Seq[CopyVioSource] = {
    Json.parse(body).validate(sourcesReads).get
  }
}


object CopyVio extends WithBot {

  val host = MwBot.ukWiki

  def pagesWithTemplate(template: String): Future[Seq[Long]] = {
    val action = Action(Query(
      Prop(
        Info(InProp(SubjectId)),
        Revisions()
      ),
      Generator(EmbeddedIn(
        EiTitle("Template:" + template),
        EiLimit("500")
      ))
    ))

    new DslQuery(action, bot).run().map {
      pages =>
        pages.map(p => p.subjectId.getOrElse(p.id.get))
    }
  }

  def pagesByIds(ids: Seq[Long]): Future[Seq[Page]] = {
    import org.scalawiki.dto.cmd.query.prop.rvprop._

    val action = Action(Query(
      PageIdsParam(ids),
      Prop(
        Info(),
        Revisions(
          RvProp(Ids)
          //          ,RvLimit("max")
        )
      )
    ))

    new DslQuery(action, bot).run()
  }

  def main(args: Array[String]) {
    val system = ActorSystem()
    val copyVio = new CopyVio(new HttpClientImpl(system))

    val revIdsFuture = pagesWithTemplate("Вікіпедія любить пам'ятки")
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

              val suspected = sources.filterNot(_.violation == "none")//s.violation == "suspected" || s.violation == "possible")

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