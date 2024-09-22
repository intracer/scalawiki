package org.scalawiki.wlx.query

import org.scalawiki.dto.cmd.query.Generator
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.dto.{Image, Namespace}
import org.scalawiki.query.QueryLibrary
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.{ActionBot, MwBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ImageQuery {

  def imagesFromCategory(contest: Contest): Future[Iterable[Image]]

  def imagesWithTemplate(contest: Contest): Future[Iterable[Image]]

}

class ImageQueryApi(bot: ActionBot) extends ImageQuery with QueryLibrary {

  override def imagesFromCategory(contest: Contest): Future[Iterable[Image]] = {
    val generator: Generator = Generator(
      CategoryMembers(
        CmTitle(contest.imagesCategory),
        CmNamespace(Seq(Namespace.FILE)),
        CmLimit("400")
      )
    )

    imagesByGenerator(contest, generator)
  }

  override def imagesWithTemplate(contest: Contest): Future[Iterable[Image]] =
    contest.fileTemplate
      .map { template =>
        imagesByGenerator(contest, generatorWithTemplate(template, Set(Namespace.FILE)))
      }
      .getOrElse(Future.successful(Nil))

  def imagesByGenerator(contest: Contest, generator: Generator): Future[Iterable[Image]] = {
    val specialNominationTemplates = contest.specialNominations.flatMap(_.fileTemplate).toSet
    for (pages <- bot.run(imagesByGenerator(generator, withMetadata = true)))
      yield pages.flatMap(
        Image.fromPage(contest.fileTemplate, specialNominationTemplates)
      )
  }
}

object ImageQuery {

  def create(implicit bot: ActionBot = MwBot.fromHost(MwBot.commons)): ImageQuery =
    new ImageQueryApi(bot)

}
