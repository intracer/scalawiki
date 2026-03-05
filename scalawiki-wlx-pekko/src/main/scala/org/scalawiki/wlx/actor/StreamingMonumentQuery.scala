package org.scalawiki.wlx.actor

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.scalawiki.MwBot
import org.scalawiki.dto.Namespace
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.prop._
import org.scalawiki.dto.cmd.query.prop.rvprop._
import org.scalawiki.dto.cmd.query.{Generator, Query}
import org.scalawiki.dto.cmd.query.list.{EiLimit, EiNamespace, EiTitle, EmbeddedIn}
import org.scalawiki.wlx.dto.Monument

object StreamingMonumentQuery {

  def source(contest: org.scalawiki.wlx.dto.Contest, bot: MwBot)(implicit
      system: ActorSystem
  ): Source[Monument, NotUsed] = {

    val listTemplate = contest.uploadConfigs.head.listTemplate
    val listConfig   = contest.uploadConfigs.head.listConfig
    val title        = "Template:" + listTemplate

    val action = Action(
      Query(
        Generator(
          EmbeddedIn(
            EiTitle(title),
            EiNamespace(Seq(Namespace.PROJECT, Namespace.MAIN)),
            EiLimit("100")
          )
        ),
        Prop(
          Revisions(
            RvProp(Content, Ids, Timestamp, User, UserId)
          )
        )
      )
    )

    MwSource
      .pages(action, bot)
      .mapConcat { pages =>
        pages.flatMap { page =>
          if (page.title.contains("новий АТУ")) Nil
          else
            Monument.monumentsFromText(
              page.text.getOrElse(""),
              page.title,
              listTemplate,
              listConfig
            ).toSeq
        }
      }
  }
}
