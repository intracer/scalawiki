package org.scalawiki.wlx.query

import org.scalawiki.MwBot
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.prop._
import org.scalawiki.dto.cmd.query.{PageIdsParam, Query}
import org.scalawiki.dto.{Namespace, Page}
import org.scalawiki.query.QueryLibrary
import org.scalawiki.wlx.dto.lists.OtherTemplateListConfig
import org.scalawiki.wlx.dto.{Contest, Monument}

import java.time.ZonedDateTime
import scala.collection.mutable
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

  override def byMonumentTemplateAsync(
      generatorTemplate: String,
      date: Option[ZonedDateTime] = None,
      listTemplate: Option[String] = None
  ): Future[Iterable[Monument]] = {
    val differentRegionIds = new ArrayBuffer[String]()

    val title =
      if (generatorTemplate.startsWith("Template")) generatorTemplate
      else "Template:" + generatorTemplate
    val listConfig = listTemplate.fold(defaultListConfig)(
      new OtherTemplateListConfig(_, defaultListConfig)
    )
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
        val monuments = pages.flatMap { page =>
          if (!page.title.contains("новий АТУ")) {
            val monuments = Monument.monumentsFromText(
              page.text.getOrElse(""),
              page.title,
              listTemplate.getOrElse(generatorTemplate),
              listConfig
            )
            val regionIds =
              monuments.map(_.id.split("-").init.mkString("-")).toSet
            if (regionIds.size > 1 && reportDifferentRegionIds) {
              differentRegionIds.append(
                s"* [[${page.title}]]: ${regionIds.toSeq.sorted.mkString(", ")}"
              )
            }
            monuments
          } else Nil
        }
        if (reportDifferentRegionIds) {
          Await.result(
            bot
              .page(s"Вікіпедія:${contest.name}/differentRegionIds")
              .edit(differentRegionIds.sorted.mkString("\n")),
            10.seconds
          )
        }
        monuments
      }
    } else {
      monumentsByDate(
        title,
        listTemplate.getOrElse(generatorTemplate),
        date.get
      )
    }
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
