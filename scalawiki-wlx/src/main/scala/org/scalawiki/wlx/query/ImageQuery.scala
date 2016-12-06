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

class ImageQueryCached(underlying: ImageQuery) extends ImageQuery {

  import spray.caching.{Cache, LruCache}

  val cache: Cache[Seq[Image]] = LruCache()

  override def imagesFromCategoryAsync(category: String, contest: Contest): Future[Seq[Image]] =
    cache(category) {
      underlying.imagesFromCategoryAsync(category, contest)
    }

  override def imagesWithTemplateAsync(template: String, contest: Contest): Future[Seq[Image]] =
    cache(template) {
      underlying.imagesWithTemplateAsync(template, contest)
    }
}


object ImageQuery {

  def create(db: Boolean = false, caching: Boolean = true, pickling: Boolean = false): ImageQuery = {
    val query = new ImageQueryApi

    if (caching)
      new ImageQueryCached(if (pickling) query else query) //          new ImageQueryPickling(api, contest)
    else query
  }
}