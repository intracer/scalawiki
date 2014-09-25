package client.wlx

import client.MwBot
import client.wlx.dto.{Image, Monument}

class ListFiller {

  def fillLists(monumentDb: MonumentDB, imageDb: ImageDB) {

    val monumentsWithoutImages = monumentDb.monuments.filter(_.photo.isEmpty)
    val idsToFill = monumentsWithoutImages.map(_.id).toSet

    val newIds = imageDb.ids.intersect(idsToFill)

    println(s"NewIds: ${newIds.size}")

    val monumentToFill = monumentsWithoutImages.filter(m => newIds.contains(m.id))
    val monumentsByPage = monumentToFill.groupBy(_.pageParam)

    println(s"pages: ${monumentsByPage.keySet.size}")

    val bot = MwBot.get(MwBot.ukWiki)
    val javaBot = bot.getJavaWiki
    for (title <- monumentsByPage.keySet) {
      val ids = monumentsByPage(title).map(_.id).toSet

      val pageText = javaBot.getPageText(title)

      //        .map { pageText =>
      val splitted = pageText.split("\\{\\{" + monumentDb.contest.listTemplate)

      val edited = splitted.zipWithIndex.map { case (text, index) =>
        if (index == 0 || index == splitted.size - 1)
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
      javaBot.edit(title, newText, s"adding ${ids.size} images")
      //      }
    }
  }

  def bestImage(images: Seq[Image]) =
    images.sortBy(image => image.size + image.width * image.height).last


}
