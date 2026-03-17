package org.scalawiki.wlx.query

import org.scalawiki.MwBot
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.prop._
import org.scalawiki.dto.cmd.query.{PageIdsParam, Query}
import org.scalawiki.dto.{Namespace, Page}
import org.scalawiki.query.QueryLibrary
import org.scalawiki.wlx.WlxTemplateParser
import org.scalawiki.wlx.dto.lists.OtherTemplateListConfig
import org.scalawiki.wlx.dto.{Contest, Monument}

import java.time.ZonedDateTime
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, _}

trait MonumentQuery {
  val Timeout = 2.minutes

  def contest: Contest

  def defaultListTemplate: String = contest.uploadConfigs.head.listTemplate

  def byMonumentTemplateAsync(
      generatorTemplate: String = defaultListTemplate,
      date: Option[ZonedDateTime] = None,
      listTemplate: Option[String] = None
  ): Future[Iterable[Monument]]

  def byMonumentTemplateMapsAsync(
      generatorTemplate: String = defaultListTemplate,
      date: Option[ZonedDateTime] = None,
      listTemplate: Option[String] = None
  ): Future[Iterable[Map[String, String]]]

  final def byMonumentTemplateMaps(
      generatorTemplate: String = defaultListTemplate,
      date: Option[ZonedDateTime] = None,
      listTemplate: Option[String] = None
  ): Iterable[Map[String, String]] =
    Await.result(
      byMonumentTemplateMapsAsync(generatorTemplate, date, listTemplate),
      Timeout
    )

  def byPageAsync(
      page: String,
      template: String,
      date: Option[ZonedDateTime] = None
  ): Future[Iterable[Monument]]

  final def byMonumentTemplate(
      generatorTemplate: String = defaultListTemplate,
      date: Option[ZonedDateTime] = None,
      listTemplate: Option[String] = None
  ): Iterable[Monument] =
    Await.result(
      byMonumentTemplateAsync(generatorTemplate, date, listTemplate),
      Timeout
    )

  final def byPage(page: String, template: String): Iterable[Monument] =
    Await.result(byPageAsync(page, template), Timeout)
}

class MonumentQueryApi(
    val contest: Contest,
    reportDifferentRegionIds: Boolean = false
)(implicit val bot: MwBot)
    extends MonumentQuery
    with QueryLibrary {

  val host = getHost.get

  val defaultListConfig = contest.uploadConfigs.head.listConfig

  def getHost: Option[String] = contest.listsHost

  /** Shared page-fetching logic for both Monument parsing and raw-map extraction.
    * Does NOT include reportDifferentRegionIds side-effects — those stay in byMonumentTemplateAsync.
    *
    * @param parser (pageName, wikiText) => Iterable[T] — applied per page
    */
  private def byMonumentTemplateGeneric[T](
      generatorTemplate: String,
      date: Option[ZonedDateTime],
      listTemplate: Option[String],
      parser: (String, String) => Iterable[T]
  ): Future[Iterable[T]] = {
    val title =
      if (generatorTemplate.startsWith("Template")) generatorTemplate
      else "Template:" + generatorTemplate

    if (date.isEmpty) {
      bot
        .page(title)
        .revisionsByGenerator(
          "embeddedin",
          "ei",
          Set(Namespace.PROJECT, Namespace.MAIN),
          Set("ids", "content", "timestamp", "user", "userid", "comment"),
          None,
          "100"
        ) map { pages =>
        pages.flatMap { page =>
          if (!page.title.contains("новий АТУ"))
            parser(page.title, page.text.getOrElse(""))
          else Nil
        }
      }
    } else {
      articlesWithTemplate(title).flatMap { ids =>
        Future.traverse(ids)(id => pageRevisions(id, date.get)).map { pages =>
          pages.flatten.flatMap(page =>
            parser(page.title, page.text.getOrElse(""))
          )
        }
      }
    }
  }

  override def byMonumentTemplateAsync(
      generatorTemplate: String,
      date: Option[ZonedDateTime] = None,
      listTemplate: Option[String] = None
  ): Future[Iterable[Monument]] = {
    val differentRegionIds = new ArrayBuffer[String]()
    val listConfig = listTemplate.fold(defaultListConfig)(
      new OtherTemplateListConfig(_, defaultListConfig)
    )
    val template = listTemplate.getOrElse(generatorTemplate)

    byMonumentTemplateGeneric(
      generatorTemplate,
      date,
      listTemplate,
      (page, text) => {
        val monuments = Monument.monumentsFromText(text, page, template, listConfig)
        if (date.isEmpty) {
          val regionIds = monuments.map(_.id.split("-").init.mkString("-")).toSet
          if (regionIds.size > 1 && reportDifferentRegionIds) {
            differentRegionIds.append(
              s"* [[$page]]: ${regionIds.toSeq.sorted.mkString(", ")}"
            )
          }
        }
        monuments
      }
    ).map { monuments =>
      if (date.isEmpty && reportDifferentRegionIds) {
        Await.result(
          bot
            .page(s"Вікіпедія:${contest.name}/differentRegionIds")
            .edit(differentRegionIds.sorted.mkString("\n")),
          10.seconds
        )
      }
      monuments
    }
  }

  override def byMonumentTemplateMapsAsync(
      generatorTemplate: String,
      date: Option[ZonedDateTime] = None,
      listTemplate: Option[String] = None
  ): Future[Iterable[Map[String, String]]] = {
    val listConfig = listTemplate.fold(defaultListConfig)(
      new OtherTemplateListConfig(_, defaultListConfig)
    )
    byMonumentTemplateGeneric(
      generatorTemplate,
      date,
      listTemplate,
      (page, text) => new WlxTemplateParser(listConfig, page).parseToMaps(text)
    )
  }

  override def byPageAsync(
      page: String,
      template: String,
      date: Option[ZonedDateTime] = None
  ): Future[Iterable[Monument]] = {
    val config = new OtherTemplateListConfig(template, defaultListConfig)
    if (!page.startsWith("Template")) {
      bot
        .page(page)
        .revisions(
          Set.empty,
          Set("content", "timestamp", "user", "userid", "comment")
        )
        .map { revs =>
          revs.headOption
            .map(page =>
              Monument
                .monumentsFromText(
                  page.text.getOrElse(""),
                  page.title,
                  template,
                  config
                )
                .toSeq
            )
            .getOrElse(Seq.empty)
        }
    } else {
      byMonumentTemplateAsync(page, date, Some(template))
    }
  }

  def monumentsByDate(
      page: String,
      template: String,
      date: ZonedDateTime
  ): Future[Iterable[Monument]] = {
    articlesWithTemplate(page).flatMap { ids =>
      Future.traverse(ids)(id => pageRevisions(id, date)).map { pages =>
        pages.flatten.flatMap(page =>
          Monument.monumentsFromText(
            page.text.getOrElse(""),
            page.title,
            template,
            defaultListConfig
          )
        )
      }
    }
  }

  def pageRevisions(id: Long, date: ZonedDateTime): Future[Option[Page]] = {
    import org.scalawiki.dto.cmd.query.prop.rvprop._

    val action = Action(
      Query(
        PageIdsParam(Seq(id)),
        Prop(
          Info(),
          Revisions(
            RvProp(Content, Ids, Size, User, UserId, Timestamp),
            RvLimit("max"),
            RvStart(date)
          )
        )
      )
    )

    bot.run(action).map { pages =>
      pages.headOption
    }
  }

}

object MonumentQuery {

  def create(contest: Contest, reportDifferentRegionIds: Boolean = false)(
      implicit bot: MwBot = MwBot.fromHost(MwBot.ukWiki)
  ): MonumentQuery =
    new MonumentQueryApi(contest, reportDifferentRegionIds)(bot)

}
