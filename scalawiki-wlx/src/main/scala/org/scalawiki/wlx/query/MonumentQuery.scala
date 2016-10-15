package org.scalawiki.wlx.query

import org.joda.time.DateTime
import org.scalawiki.WithBot
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.list.{EiLimit, EiTitle, EmbeddedIn}
import org.scalawiki.dto.cmd.query.prop._
import org.scalawiki.dto.cmd.query.{Generator, PageIdsParam, Query}
import org.scalawiki.dto.{Namespace, Page}
import org.scalawiki.wlx.dto.lists.OtherTemplateListConfig
import org.scalawiki.wlx.dto.{Contest, Monument}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, _}

trait MonumentQuery {

  import scala.concurrent.duration._

  def contest: Contest

  def byMonumentTemplateAsync(template: String = contest.uploadConfigs.head.listTemplate, date: Option[DateTime] = None): Future[Seq[Monument]]

  def byPageAsync(page: String, template: String, pageIsTemplate: Boolean = false, date: Option[DateTime] = None): Future[Seq[Monument]]

  final def byMonumentTemplate(template: String = contest.uploadConfigs.head.listTemplate, date: Option[DateTime] = None) =
    Await.result(byMonumentTemplateAsync(template, date), 120.minutes): Seq[Monument]

  final def byPage(page: String, template: String, pageIsTemplate: Boolean = false) =
    Await.result(byPageAsync(page, template, pageIsTemplate), 15.minutes): Seq[Monument]
}

class MonumentQueryApi(val contest: Contest) extends MonumentQuery with WithBot {

  val host = getHost.get

  val listConfig = contest.uploadConfigs.head.listConfig

  def getHost = contest.listsHost

  override def byMonumentTemplateAsync(template: String, date: Option[DateTime] = None): Future[Seq[Monument]] = {

    if (date.isEmpty) {

      bot.page("Template:" + template).revisionsByGenerator("embeddedin", "ei",
        Set(Namespace.PROJECT, Namespace.MAIN),
        Set("ids", "content", "timestamp", "user", "userid", "comment"), None, "100") map {
        pages =>
          pages.flatMap(page =>
            Monument.monumentsFromText(page.text.getOrElse(""), page.title, template, listConfig))
      }
    } else {
      monumentsByDate("Template:" + template, template, date.get)
    }
  }

  override def byPageAsync(page: String, template: String, pageIsTemplate: Boolean = false, date: Option[DateTime] = None): Future[Seq[Monument]] = {
    val config = new OtherTemplateListConfig(template, listConfig)
    if (!page.startsWith("Template") || pageIsTemplate) {
      bot.page(page).revisions(Set.empty, Set("content", "timestamp", "user", "userid", "comment")).map {
        revs =>
          revs.headOption.map(page =>
            Monument.monumentsFromText(page.text.getOrElse(""), page.title, template, config).toSeq).getOrElse(Seq.empty)
      }
    } else {
      //      bot.page(page).revisionsByGenerator("links", null, Set.empty, Set("content", "timestamp", "user", "comment")).map {
      //        pages =>
      //          pages.flatMap(page => Monument.monumentsFromText(page.text.getOrElse(""), page.title, template).toSeq)
      //      }
      if (date.isEmpty) {
        bot.page(page).revisionsByGenerator(
          "embeddedin", "ei", Set(Namespace.PROJECT), Set("content", "timestamp", "user", "userid", "comment"), None, "100"
        ).map {
          pages =>
            pages.flatMap(page => Monument.monumentsFromText(page.text.getOrElse(""), page.title, template, config))
        }
      } else {
        monumentsByDate(page, template, date.get)
      }
    }
  }

  def monumentsByDate(page: String, template: String, date: DateTime): Future[Seq[Monument]] = {
    pagesWithTemplate(page).flatMap {
      ids =>
        Future.traverse(ids)(id => pageRevisions(id, date)).map {
          pages =>
            pages.flatten.flatMap(page => Monument.monumentsFromText(page.text.getOrElse(""), page.title, template, listConfig))
        }
    }
  }

  def pagesWithTemplate(template: String): Future[Seq[Long]] = {
    val action = Action(Query(
      Prop(
        Info(InProp(SubjectId)),
        Revisions()
      ),
      Generator(EmbeddedIn(
        EiTitle(template),
        EiLimit("500")
      ))
    ))

    bot.run(action).map {
      pages =>
        pages.map(p => p.subjectId.getOrElse(p.id.get))
    }
  }


  def pageRevisions(id: Long, date: DateTime): Future[Option[Page]] = {
    import org.scalawiki.dto.cmd.query.prop.rvprop._

    val action = Action(Query(
      PageIdsParam(Seq(id)),
      Prop(
        Info(),
        Revisions(
          RvProp(Content, Ids, Size, User, UserId, Timestamp),
          RvLimit("max"),
          RvStart(date)
        )
      )
    ))

    bot.run(action).map { pages =>
      pages.headOption
    }
  }

}

class MonumentQueryCached(underlying: MonumentQuery) extends MonumentQuery {

  override def contest = underlying.contest

  import spray.caching.{Cache, LruCache}

  val cache: Cache[Seq[Monument]] = LruCache()

  override def byMonumentTemplateAsync(template: String, date: Option[DateTime] = None): Future[Seq[Monument]] = cache(template) {
    underlying.byMonumentTemplateAsync(template, date)
  }

  override def byPageAsync(page: String, template: String, pageIsTemplate: Boolean = false, date: Option[DateTime] = None): Future[Seq[Monument]] = cache(page) {
    underlying.byPageAsync(page, template: String, pageIsTemplate, date)
  }
}


object MonumentQuery {

  def create(contest: Contest, caching: Boolean = false, pickling: Boolean = false): MonumentQuery = {
    val api = new MonumentQueryApi(contest)

    if (caching)
      new MonumentQueryCached(api)
    else api
  }

}

