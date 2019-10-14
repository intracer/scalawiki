package org.scalawiki.wlx.query

import java.time.ZonedDateTime

import org.scalawiki.{ActionBot, MwBot, WithBot}
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.prop._
import org.scalawiki.dto.cmd.query.{PageIdsParam, Query}
import org.scalawiki.dto.{Namespace, Page}
import org.scalawiki.query.QueryLibrary
import org.scalawiki.wlx.dto.lists.OtherTemplateListConfig
import org.scalawiki.wlx.dto.{Contest, Monument}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, _}

trait MonumentQuery {

  import scala.concurrent.duration._

  def contest: Contest

  def byMonumentTemplateAsync(template: String = contest.uploadConfigs.head.listTemplate, date: Option[ZonedDateTime] = None): Future[Seq[Monument]]

  def byPageAsync(page: String, template: String, pageIsTemplate: Boolean = false, date: Option[ZonedDateTime] = None): Future[Seq[Monument]]

  final def byMonumentTemplate(template: String = contest.uploadConfigs.head.listTemplate, date: Option[ZonedDateTime] = None) =
    Await.result(byMonumentTemplateAsync(template, date), 120.minutes): Seq[Monument]

  final def byPage(page: String, template: String, pageIsTemplate: Boolean = false) =
    Await.result(byPageAsync(page, template, pageIsTemplate), 15.minutes): Seq[Monument]
}

class MonumentQueryApi(val contest: Contest)(implicit val bot: MwBot) extends MonumentQuery with QueryLibrary {

  val host = getHost.get

  val listConfig = contest.uploadConfigs.head.listConfig

  def getHost = contest.listsHost

  override def byMonumentTemplateAsync(template: String, date: Option[ZonedDateTime] = None): Future[Seq[Monument]] = {

    if (date.isEmpty) {

      bot.page("Template:" + template).revisionsByGenerator("embeddedin", "ei",
        Set(Namespace.PROJECT, Namespace.MAIN),
        Set("ids", "content", "timestamp", "user", "userid", "comment"), None, "100") map { pages =>
          pages.flatMap(page =>
            Monument.monumentsFromText(page.text.getOrElse(""), page.title, template, listConfig))
      }
    } else {
      monumentsByDate("Template:" + template, template, date.get)
    }
  }

  override def byPageAsync(page: String, template: String, pageIsTemplate: Boolean = false, date: Option[ZonedDateTime] = None): Future[Seq[Monument]] = {
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

  def monumentsByDate(page: String, template: String, date: ZonedDateTime): Future[Seq[Monument]] = {
    articlesWithTemplate(page).flatMap {
      ids =>
        Future.traverse(ids)(id => pageRevisions(id, date)).map {
          pages =>
            pages.flatten.flatMap(page => Monument.monumentsFromText(page.text.getOrElse(""), page.title, template, listConfig))
        }
    }
  }

  def pageRevisions(id: Long, date: ZonedDateTime): Future[Option[Page]] = {
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

object MonumentQuery {

  def create(contest: Contest)(implicit bot: MwBot = MwBot.fromHost(MwBot.ukWiki)): MonumentQuery =
    new MonumentQueryApi(contest)(bot)

}

