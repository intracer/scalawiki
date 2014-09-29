package client.wlx

import client.MwBot
import client.wlx.dto.{Image, Monument}

import scala.collection.immutable.SortedSet

class ListFiller {
  import scala.concurrent.ExecutionContext.Implicits.global

  def fillLists(monumentDb: MonumentDB, imageDb: ImageDB) {

    val monumentsWithoutImages = monumentDb.monuments.filter(_.photo.isEmpty)
    val idsToFill = monumentsWithoutImages.map(_.id).toSet

    val newIds = imageDb.ids.intersect(idsToFill)

    println(s"NewIds: ${newIds.size}")

    val monumentToFill = monumentsWithoutImages.filter(m => newIds.contains(m.id))
    val monumentsByPage = monumentToFill.groupBy(_.pageParam)

    val titles = SortedSet(monumentsByPage.keys.toSeq: _*)
    println(s"pages: ${titles.size}")

    val bot = MwBot.get(MwBot.ukWiki)
    //val javaBot = bot.getJavaWiki
    for (title <- titles) {
      val ids = monumentsByPage(title).map(_.id).toSet

      for (pageText <- bot.pageText(title)) {
        val splitted = pageText.split("\\{\\{" + monumentDb.contest.listTemplate)

        val edited = splitted.zipWithIndex.map { case (text, index) =>
          if (index == 0)
            text
          else {
            val monument = Monument.init(text, title)

            if (!ids.contains(monument.id))
              text
            else {
              monument.setTemplateParam("фото", bestImage(imageDb.byId(monument.id)).title).text.replaceFirst("File:", "")
            }
          }
        }

        val newText = edited.mkString("{{" + monumentDb.contest.listTemplate)
        val comment = s"adding ${ids.size} image(s)"
        bot.page(title).edit(newText, comment)
  //      javaBot.edit(title, newText, comment)
      }
    }
  }

  def bestImage(images: Seq[Image]) =
    images.sortBy(image => image.size + image.width * image.height).last


}
