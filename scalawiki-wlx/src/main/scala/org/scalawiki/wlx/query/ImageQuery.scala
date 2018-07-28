package org.scalawiki.wlx.query

import org.scalawiki.dto.cmd.query.Generator
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.dto.{Image, Namespace}
import org.scalawiki.query.QueryLibrary
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.{MwBot, WithBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ImageQuery {

  def imagesFromCategoryAsync(category: String, contest: Contest): Future[Seq[Image]]

  def imagesWithTemplateAsync(template: String, contest: Contest): Future[Seq[Image]]

}

class ImageQueryApi extends ImageQuery with WithBot with QueryLibrary {

  val host = MwBot.commons

  override def imagesFromCategoryAsync(category: String, contest: Contest): Future[Seq[Image]] = {
    val generator: Generator = Generator(CategoryMembers(CmTitle(category), CmNamespace(Seq(Namespace.FILE)), CmLimit("500"))) // 5000 / 10

    imagesByGenerator(contest, generator)
  }

  def imagesByGenerator(contest: Contest, generator: Generator): Future[Seq[Image]] = {

    bot.run(imagesByGenerator(generator)).map {
      pages => pages.map {
        page =>

          val fromRev = Image.fromPageRevision(page, contest.fileTemplate)
          val fromImage = Image.fromPageImages(page, contest.fileTemplate)

          fromImage.get.copy(
            monumentId = fromRev.flatMap(_.monumentId),
            author = fromRev.flatMap(_.author)
          )
      }
    }
  }

  override def imagesWithTemplateAsync(template: String, contest: Contest): Future[Seq[Image]] =
    imagesByGenerator(contest, generatorWithTemplate(template, Set(Namespace.FILE)))

}

object ImageQuery {

  def create(db: Boolean = false): ImageQuery = new ImageQueryApi

}