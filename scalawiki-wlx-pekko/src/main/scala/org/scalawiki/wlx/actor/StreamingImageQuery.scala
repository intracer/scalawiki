package org.scalawiki.wlx.actor

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.scalawiki.MwBot
import org.scalawiki.dto.{Image, Namespace}
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.prop._
import org.scalawiki.dto.cmd.query.prop.iiprop.{IiProp, Size, Url}
import org.scalawiki.dto.cmd.query.prop.rvprop._
import org.scalawiki.dto.cmd.query.{Generator, Query}
import org.scalawiki.dto.cmd.query.list.{EiLimit, EiNamespace, EiTitle, EmbeddedIn}
import org.scalawiki.wlx.dto.Contest

object StreamingImageQuery {

  def source(contest: Contest, bot: MwBot)(implicit
      system: ActorSystem
  ): Source[Image, NotUsed] = {
    contest.fileTemplate match {
      case None => Source.empty[Image]

      case Some(template) =>
        val specialNominationTemplates =
          contest.specialNominations.flatMap(_.fileTemplate).toSet

        val action = Action(
          Query(
            Generator(
              EmbeddedIn(
                EiTitle("Template:" + template),
                EiNamespace(Seq(Namespace.FILE)),
                EiLimit("50")
              )
            ),
            Prop(
              Info(),
              Revisions(
                RvProp(Content, Ids, Timestamp, User, UserId)
              ),
              ImageInfo(IiProp(iiprop.Timestamp, iiprop.User, Size, Url))
            )
          )
        )

        MwSource
          .pages(action, bot)
          .mapConcat { pages =>
            pages.flatMap(
              Image.fromPage(contest.fileTemplate, specialNominationTemplates)
            )
          }
    }
  }
}
