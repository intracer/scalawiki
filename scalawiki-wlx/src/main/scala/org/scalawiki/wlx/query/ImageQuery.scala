package org.scalawiki.wlx.query

import org.scalawiki.dto.cmd.query.Generator
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.dto.{Image, Namespace}
import org.scalawiki.query.QueryLibrary
import org.scalawiki.wlx.dto.{Contest, SpecialNomination}
import org.scalawiki.{ActionBot, MwBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ImageQuery {

  def imagesFromCategoryAsync(
      category: String,
      contest: Contest
  ): Future[Iterable[Image]]

  def imagesWithTemplateAsync(
      template: String,
      contest: Contest
  ): Future[Iterable[Image]]

}

class ImageQueryApi(bot: ActionBot) extends ImageQuery with QueryLibrary {

  override def imagesFromCategoryAsync(
      category: String,
      contest: Contest
  ): Future[Iterable[Image]] = {
    val generator: Generator = Generator(
      CategoryMembers(
        CmTitle(category),
        CmNamespace(Seq(Namespace.FILE)),
        CmLimit("400")
      )
    ) // 5000 / 10

    imagesByGenerator(contest, generator)
  }

  override def imagesWithTemplateAsync(
      template: String,
      contest: Contest
  ): Future[Iterable[Image]] = {
    imagesByGenerator(
      contest,
      generatorWithTemplate(template, Set(Namespace.FILE))
    )
  }

  def imagesByGenerator(
      contest: Contest,
      generator: Generator
  ): Future[Iterable[Image]] = {
    val specialNominationTemplates = SpecialNomination.nominations
      .filter(n => n.years.contains(contest.year))
      .flatMap(_.fileTemplate)
    for (pages <- bot.run(imagesByGenerator(generator))) yield {
      val optionalImages =
        for (page <- pages)
          yield Image.fromPage(
            page,
            contest.fileTemplate,
            specialNominationTemplates
          )
      optionalImages.flatten
    }
  }
}

object ImageQuery {

  def create(db: Boolean = false)(implicit
      bot: ActionBot = MwBot.fromHost(MwBot.commons)
  ): ImageQuery = new ImageQueryApi(bot)

}
