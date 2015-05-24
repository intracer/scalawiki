package org.scalawiki.wlx.query

import org.scalawiki.dto.Namespace
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.dto.cmd.query.prop.iiprop.{IiProp, Timestamp}
import org.scalawiki.dto.cmd.query.prop.rvprop.RvProp
import org.scalawiki.dto.cmd.query.{Generator, Query}
import org.scalawiki.query.{DslQueryDbCache, DslQuery}
import org.scalawiki.wlx.dto.{Contest, Image}
import org.scalawiki.{MwBot, WithBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, _}

trait ImageQuery {

  import scala.concurrent.duration._


  def imagesFromCategoryAsync(category: String, contest: Contest): Future[Seq[Image]]

  def imagesWithTemplateAsync(template: String, contest: Contest): Future[Seq[Image]]

  final private[this] def imagesFromCategory(category: String, contest: Contest): Seq[Image] =
    Await.result(imagesFromCategoryAsync(category, contest), 30.minutes)

  final private[this] def imagesWithTemplate(template: String, contest: Contest): Seq[Image] =
    Await.result(imagesWithTemplateAsync(template, contest), 30.minutes)

}

class ImageQueryApi extends ImageQuery with WithBot {

  val host = MwBot.commons

  override def imagesFromCategoryAsync(category: String, contest: Contest): Future[Seq[Image]] = {
    val generator: Generator = Generator(CategoryMembers(CmTitle(category), CmNamespace(Seq(Namespace.FILE)), CmLimit("500"))) // 5000 / 10

    imagesByGenerator(contest, generator)
  }

  def imagesByGenerator(contest: Contest, generator: Generator): Future[Seq[Image]] = {
    import org.scalawiki.dto.cmd.query.prop._
    val action = Action(Query(
      Prop(
        Info(),
        Revisions(RvProp(rvprop.Ids, rvprop.Content, rvprop.Timestamp, rvprop.User, rvprop.UserId)),
        ImageInfo(
          IiProp(Timestamp, iiprop.User, iiprop.Size, iiprop.Url)
        )
      ),
      generator
    ))

    val future = new DslQueryDbCache(new DslQuery(action, bot)).run()

    future.map {
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

  override def imagesWithTemplateAsync(template: String, contest: Contest): Future[Seq[Image]] = {
    val generator: Generator = Generator(EmbeddedIn(EiTitle("Template:" + template), EiNamespace(Seq(Namespace.FILE)), EiLimit("500")))

    imagesByGenerator(contest, generator)
  }
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

  def create(db: Boolean = true, caching: Boolean = true, pickling: Boolean = false): ImageQuery = {
    val query = if (db)
      new ImageQueryApi
    else
      new ImageQueryApi

    val wrapper = if (caching)
      new ImageQueryCached(if (pickling) query else query) //          new ImageQueryPickling(api, contest)
    else query

    wrapper
  }
}