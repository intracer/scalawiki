package client.wlx

import client.dto.{PageQuery, Namespace, SinglePageQuery, Template}
import client.{LoginInfo, MwBot}

class ImageDB(val contest: Contest) {

  var bot: MwBot = _

  var images: Seq[Image] = Seq.empty

  var _byId: Map[String, Seq[Image]] = Map.empty

  def ids: Set[String] = _byId.keySet

  def initBot() = {
    bot = MwBot.create("commons.wikimedia.org")
    bot.await(bot.login(LoginInfo.login, LoginInfo.password))
  }

  def fetchImages() {
    images = imagesFromCategory()
    _byId = images.groupBy(_.monumentId.getOrElse(""))
  }

  def byId(id: String) = _byId.getOrElse(id, Seq.empty[Image])

  def byRegion(regId: String) = images.filter(_.monumentId.fold(false)(_.startsWith(regId)))

  def imagesFromCategory():Seq[Image] = {
    val query = bot.page(contest.category)
    query.imageInfoByGenerator("categorymembers", "cm", Set(Namespace.FILE_NAMESPACE)).map {
      filesInCategory =>
        val newImages: Seq[Image] = filesInCategory.flatMap(page => Image.fromPage(page, contest)).sortBy(_.pageId)

        Some(contest.listTemplate).fold(newImages) { monumentIdTemplate =>
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

}
