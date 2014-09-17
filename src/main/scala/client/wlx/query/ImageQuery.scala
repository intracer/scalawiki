package client.wlx.query

import client.dto.{Namespace, Template}
import client.wlx.dto.{Contest, Image}
import client.{LoginInfo, MwBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent._

trait ImageQuery {

  def imagesFromCategoryAsync(category:String, contest: Contest):Future[Seq[Image]]
  def imagesWithTemplateAsync(template:String, contest: Contest):Future[Seq[Image]]

  def imagesFromCategory(category:String, contest: Contest):Seq[Image]
  def imagesWithTemplate(template:String, contest: Contest):Seq[Image]
}

class ImageQueryApi extends ImageQuery{


  var bot: MwBot = _

  def initBot() = {
    bot = MwBot.create("commons.wikimedia.org")
    bot.await(bot.login(LoginInfo.login, LoginInfo.password))
  }

  override def imagesFromCategoryAsync(category:String, contest: Contest):Future[Seq[Image]] = {
    val query = bot.page(category)
    query.imageInfoByGenerator("categorymembers", "cm", Set(Namespace.FILE_NAMESPACE)).map {
      filesInCategory =>
        val newImages: Seq[Image] = filesInCategory.flatMap(page => Image.fromPage(page)).sortBy(_.pageId)

        Some(contest.fileTemplate).fold(newImages) { monumentIdTemplate =>
          bot.await(query.revisionsByGenerator("categorymembers", "cm",
            Set.empty, Set("content", "timestamp", "user", "comment")) map {
            pages =>

              val idRegex = """(\d\d)-(\d\d\d)-(\d\d\d\d)"""
              val ids: Seq[Option[String]] = pages.sortBy(_.pageid)
                .flatMap(_.text.map(Template.getDefaultParam(_, monumentIdTemplate)))
                //                .map(id => if (id.matches(idRegex)) Some(id) else Some(id))
                .map(id => if (id.isEmpty) None else Some(id))

              val imagesWithIds = newImages.zip(ids).map {
                case (image, Some(id)) => image.copy(monumentId = Some(id))
                case (image, None) => image
              }
              imagesWithIds
          })
        }
    }
  }

  override def imagesWithTemplateAsync(template:String, contest: Contest):Future[Seq[Image]] = {
    val query = bot.page("Template:" + template)
    query.imageInfoByGenerator("embeddedin", "ei", Set(Namespace.FILE_NAMESPACE)).map {
      filesInCategory =>
        val newImages: Seq[Image] = filesInCategory.flatMap(page => Image.fromPage(page)).sortBy(_.pageId)

        Some(contest.fileTemplate).fold(newImages) { monumentIdTemplate =>
          bot.await(query.revisionsByGenerator("embeddedin", "ei",
            Set.empty, Set("content", "timestamp", "user", "comment")) map {
            pages =>

              val idRegex = """(\d\d)-(\d\d\d)-(\d\d\d\d)"""
              val ids: Seq[Option[String]] = pages.sortBy(_.pageid)
                .flatMap(_.text.map(Template.getDefaultParam(_, monumentIdTemplate)))
                //                .map(id => if (id.matches(idRegex)) Some(id) else Some(id))
                .map(id => if (id.isEmpty) None else Some(id))

              val imagesWithIds = newImages.zip(ids).map {
                case (image, Some(id)) => image.copy(monumentId = Some(id))
                case (image, None) => image
              }
              imagesWithIds
          })
        }
    }
  }

  override def imagesFromCategory(category:String, contest: Contest):Seq[Image] =
    bot.await(imagesFromCategoryAsync(category, contest))

  override def imagesWithTemplate(template: String, contest: Contest): Seq[Image] =
    bot.await(imagesWithTemplateAsync(template, contest))

}

class ImageQuerySeq(
                     contest: Contest,
                     imagesByCategory: Map[String, Seq[Image]],
                     imagesWithTemplate: Seq[Image]
                     ) extends ImageQuery {
  override def imagesFromCategory(category: String, contest: Contest): Seq[Image] =
    imagesByCategory.getOrElse(category, Seq.empty)

  override def imagesFromCategoryAsync(category: String, contest: Contest): Future[Seq[Image]] = future {
    imagesFromCategory(category, contest)
  }

  override def imagesWithTemplate(template: String, contest: Contest): Seq[Image] = imagesWithTemplate

  override def imagesWithTemplateAsync(template: String, contest: Contest): Future[Seq[Image]] = future {
    imagesWithTemplate(template, contest)
  }

}