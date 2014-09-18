package client.wlx.query

import java.io.File

import client.dto.Namespace
import client.wlx.WithBot
import client.wlx.dto.{Contest, Monument}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, _}

trait MonumentQuery {
  import scala.concurrent.duration._

  def listsAsync(template: String): Future[Seq[Monument]]
  final def lists(template: String) = Await.result(listsAsync(template), 15.minutes): Seq[Monument]
}

class MonumentQueryApi(contest: Contest) extends MonumentQuery with WithBot {

  val host = contest.country.languageCode + ".wikipedia.org"

  override def listsAsync(template: String): Future[Seq[Monument]] = {
    bot.page("Template:" + template).revisionsByGenerator("embeddedin", "ei", Set(Namespace.PROJECT_NAMESPACE), Set("content", "timestamp", "user", "comment"), None, "100") map {
      pages =>
        val monuments = pages.flatMap(page => Monument.monumentsFromText(page.text.getOrElse(""), page.title, template))
        monuments
    }
  }
}


class MonumentQuerySeq(monuments: Seq[Monument]) extends MonumentQuery {

  override def listsAsync(template: String): Future[Seq[Monument]] = future { monuments }

}

class MonumentQueryCached(underlying: MonumentQuery) extends MonumentQuery {

  import spray.caching.{Cache, LruCache}

  val cache: Cache[Seq[Monument]] = LruCache()

  override def listsAsync(template: String): Future[Seq[Monument]] = cache(template) {
    underlying.listsAsync(template)
  }
}


class MonumentQueryPickling(underlying: MonumentQuery, contest: Contest) extends MonumentQuery {

  import scala.pickling._
//  import scala.pickling.json._   // :( Slow parsing
  import java.nio.file.Files

import scala.pickling.binary._  // :( exception with unpickling

  override def listsAsync(template: String): Future[Seq[Monument]]
  = future {
      val file = new File(s"cache/monuments_${contest.contestType.code}_${contest.country.code}_${contest.year}.bin")
      if (file.exists()) {
        val data = Files.readAllBytes(file.toPath)
        data.unpickle[Seq[Monument]]
      } else {
        val list = underlying.lists(template)
        val data = list.pickle.value
        Files.write(file.toPath, data)
        list
      }
  }

}


