package client.wlx.query

import client.wlx.WithBot
import client.wlx.dto.{Contest, Image}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, _}

trait ImageQuery {
  import scala.concurrent.duration._


  def imagesFromCategoryAsync(category:String, contest: Contest):Future[Seq[Image]]
  def imagesWithTemplateAsync(template:String, contest: Contest):Future[Seq[Image]]

  final def imagesFromCategory(category:String, contest: Contest):Seq[Image] =
    Await.result(imagesFromCategoryAsync(category, contest), 30.minutes)

  final def imagesWithTemplate(template:String, contest: Contest):Seq[Image] =
    Await.result(imagesWithTemplateAsync(template, contest), 30.minutes)

}

class ImageQueryApi extends ImageQuery with WithBot {

  val host = "commons.wikimedia.org"

  override def imagesFromCategoryAsync(category: String, contest: Contest): Future[Seq[Image]] = {
    val query = bot.page(category)

    query.revisionsByGenerator("categorymembers", "cm",
      Set.empty, Set("content", "timestamp", "user", "comment")) map {
      pages =>
        pages.flatMap(page => Image.fromPageRevision(page, contest.fileTemplate)).sortBy(_.pageId)
    }
  }

  override def imagesWithTemplateAsync(template: String, contest: Contest): Future[Seq[Image]] = {
    val query = bot.page("Template:" + template)

    query.revisionsByGenerator("embeddedin", "ei",
      Set.empty, Set("content", "timestamp", "user", "comment")) map {
      pages =>
        pages.flatMap(page => Image.fromPageRevision(page, contest.fileTemplate)).sortBy(_.pageId)
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

  def create(caching: Boolean = true, pickling: Boolean = false):ImageQuery = {
    val api = new ImageQueryApi

    val query = if (caching)
      new ImageQueryCached(
        if (pickling)
          api
        //          new ImageQueryPickling(api, contest)
        else api
      )
    else api

    query
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