package client.wlx.query

import java.io.File

import client.dto.Namespace
import client.wlx.WithBot
import client.wlx.dto.{Contest, Monument}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, _}

trait MonumentQuery {
  import scala.concurrent.duration._

  def byMonumentTemplateAsync(template: String): Future[Seq[Monument]]
  def byPageAsync(page: String, template: String): Future[Seq[Monument]]

  final def byMonumentTemplate(template: String) = Await.result(byMonumentTemplateAsync(template), 15.minutes): Seq[Monument]
  final def byPage(page: String, template: String) = Await.result(byMonumentTemplateAsync(page), 15.minutes): Seq[Monument]
}

class MonumentQueryApi(contest: Contest) extends MonumentQuery with WithBot {

  val host = contest.country.languageCode + ".wikipedia.org"

  override def byMonumentTemplateAsync(template: String): Future[Seq[Monument]] = {
    bot.page("Template:" + template).revisionsByGenerator("embeddedin", "ei", Set(Namespace.PROJECT_NAMESPACE), Set("content", "timestamp", "user", "comment"), None, "100") map {
      pages =>
        pages.flatMap(page => Monument.monumentsFromText(page.text.getOrElse(""), page.title, template))
    }
  }

  override def byPageAsync(page: String, template: String): Future[Seq[Monument]] = bot.page(page).revisions().map {
    revs =>
      revs.headOption.map(page => Monument.monumentsFromText(page.text.getOrElse(""), page.title, template).toSeq).getOrElse(Seq.empty)
  }
}


class MonumentQuerySeq(monuments: Seq[Monument]) extends MonumentQuery {

  override def byMonumentTemplateAsync(template: String): Future[Seq[Monument]] = future { monuments }

  override def byPageAsync(page: String, template: String): Future[Seq[Monument]] = future { monuments }
}

class MonumentQueryCached(underlying: MonumentQuery) extends MonumentQuery {

  import spray.caching.{Cache, LruCache}

  val cache: Cache[Seq[Monument]] = LruCache()

  override def byMonumentTemplateAsync(template: String): Future[Seq[Monument]] = cache(template) {
    underlying.byMonumentTemplateAsync(template)
  }

  override def byPageAsync(page: String, template: String): Future[Seq[Monument]] = cache(page) {
    underlying.byPageAsync(page, template: String)
  }
}


class MonumentQueryPickling(underlying: MonumentQuery, contest: Contest) extends MonumentQuery {

  import scala.pickling._
//  import scala.pickling.json._   // :( Slow parsing
  import java.nio.file.Files

import scala.pickling.binary._  // :( exception with unpickling

  override def byMonumentTemplateAsync(template: String): Future[Seq[Monument]]
  = future {
      val file = new File(s"cache/monuments_${contest.contestType.code}_${contest.country.code}_${contest.year}.bin")
      if (file.exists()) {
        val data = Files.readAllBytes(file.toPath)
        data.unpickle[Seq[Monument]]
      } else {
        val list = underlying.byMonumentTemplate(template)
        val data = list.pickle.value
        Files.write(file.toPath, data)
        list
      }
  }

  override def byPageAsync(page: String, template: String): Future[Seq[Monument]] = ???
}

object MonumentQuery {

  def create(contest: Contest, caching: Boolean = true, pickling: Boolean = false):MonumentQuery = {
    val api = new MonumentQueryApi(contest)

    val query = if (caching)
      new MonumentQueryCached(
        if (pickling)
          new MonumentQueryPickling(api, contest)
        else api
      )
    else api

    query
  }

}

