package client.wlx.query

import client.MwBot
import client.dto.Namespace
import client.slick.Slick
import client.wlx.WithBot
import client.wlx.dto.{Contest, Image}

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
    val query = bot.page(category)

    val revsFuture = query.revisionsByGenerator("categorymembers", "cm",
      Set.empty, Set("content", "timestamp", "user", "comment"), None, "3000") map {
      pages =>
          pages.flatMap(page => Image.fromPageRevision(page, contest.fileTemplate, contest.year.toString)).sortBy(_.pageId)
    }

    val imageInfoFuture = query.imageInfoByGenerator("categorymembers", "cm", Set(Namespace.FILE)) map {
      pages =>
        try {
        pages.flatMap(page => Image.fromPageImageInfo(page, contest.fileTemplate, contest.year.toString)).sortBy(_.pageId)
        } catch {case e =>
          throw e
        }
    }

    for (revs <- revsFuture;
         imageInfos <- imageInfoFuture) yield {
      val revsByPageId = revs.groupBy(_.pageId)
      imageInfos.map {
        ii =>
          val revImage = revsByPageId.get(ii.pageId).map(_.head)
          ii.copy(
            monumentId = revImage.flatMap(_.monumentId),
            author = revImage.flatMap(_.author)
          )
      }
    }
  }

  override def imagesWithTemplateAsync(template: String, contest: Contest): Future[Seq[Image]] = {
    val query = bot.page("Template:" + template)

    query.revisionsByGenerator("embeddedin", "ei",
      Set(Namespace.FILE), Set("content", "timestamp", "user", "comment"), None, "3000") map {
      pages =>
       val result =  pages.flatMap(page => Image.fromPageRevision(page, contest.fileTemplate, "")).sortBy(_.pageId)
        result
    }
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

class ImageQuerySlick extends ImageQuery {

  import scala.slick.driver.H2Driver.simple._

  val slick = new Slick()

  override def imagesFromCategoryAsync(category: String, contest: Contest): Future[Seq[Image]] =
    future {
      slick.db.withSession { implicit session =>
        slick.images.list.filter(_.year == Some(contest.year.toString))
      }
    }

  override def imagesWithTemplateAsync(template: String, contest: Contest): Future[Seq[Image]] = ???

}

class ImageQuerySeq(
                     imagesByCategory: Map[String, Seq[Image]],
                     imagesWithTemplate: Seq[Image]
                     ) extends ImageQuery {

  override def imagesFromCategoryAsync(category: String, contest: Contest): Future[Seq[Image]] = future {
    imagesByCategory.getOrElse(category, Seq.empty)
  }

  override def imagesWithTemplateAsync(template: String, contest: Contest): Future[Seq[Image]] = future {
    imagesWithTemplate
  }

}

object ImageQuery {

  def create(db: Boolean = true, caching: Boolean = true, pickling: Boolean = false): ImageQuery = {
    val query = if (db)
      new ImageQuerySlick
    else
      new ImageQueryApi

    val wrapper = if (caching)
      new ImageQueryCached(if (pickling) query else query)//          new ImageQueryPickling(api, contest)
    else query

    wrapper
  }
}


//private def imagesFromCategoryAsyncFull(category:String, contest: Contest):Future[Seq[Image]] = {
//val query = bot.page(category)
//query.imageInfoByGenerator("categorymembers", "cm", Set(Namespace.FILE_NAMESPACE)).map {
//filesInCategory =>
//val newImages: Seq[Image] = filesInCategory.flatMap(page => Image.fromPageImageInfo(page)).sortBy(_.pageId)
//
//Some(contest.fileTemplate).fold(newImages) { monumentIdTemplate =>
//bot.await(query.revisionsByGenerator("categorymembers", "cm",
//Set.empty, Set("content", "timestamp", "user", "comment")) map {
//pages =>
//
//val idRegex = """(\d\d)-(\d\d\d)-(\d\d\d\d)"""
//val ids: Seq[Option[String]] = pages.sortBy(_.pageid)
//.flatMap(_.text.map(Template.getDefaultParam(_, monumentIdTemplate)))
////                .map(id => if (id.matches(idRegex)) Some(id) else Some(id))
//.map(id => if (id.isEmpty) None else Some(id))
//
//val imagesWithIds = newImages.zip(ids).map {
//case (image, Some(id)) => image.copy(monumentId = Some(id))
//case (image, None) => image
//}
//imagesWithIds
//})
//}
//}
//}
//
//private def imagesWithTemplateAsyncFull(template:String, contest: Contest):Future[Seq[Image]] = {
//val query = bot.page("Template:" + template)
//query.imageInfoByGenerator("embeddedin", "ei", Set(Namespace.FILE_NAMESPACE)).map {
//filesInCategory =>
//val newImages: Seq[Image] = filesInCategory.flatMap(page => Image.fromPageImageInfo(page)).sortBy(_.pageId)
//
//Some(contest.fileTemplate).fold(newImages) { monumentIdTemplate =>
//bot.await(query.revisionsByGenerator("embeddedin", "ei",
//Set.empty, Set("content", "timestamp", "user", "comment")) map {
//pages =>
//
//val idRegex = """(\d\d)-(\d\d\d)-(\d\d\d\d)"""
//val ids: Seq[Option[String]] = pages.sortBy(_.pageid)
//.flatMap(_.text.map(Template.getDefaultParam(_, monumentIdTemplate)))
////                .map(id => if (id.matches(idRegex)) Some(id) else Some(id))
//.map(id => if (id.isEmpty) None else Some(id))
//
//val imagesWithIds = newImages.zip(ids).map {
//case (image, Some(id)) => image.copy(monumentId = Some(id))
//case (image, None) => image
//}
//imagesWithIds
//})
//}
//}
//}