package org.scalawiki.wlx

import org.scalawiki.MwBot
import org.scalawiki.edit.{PageUpdateTask, PageUpdater}
import org.scalawiki.wlx.dto.{Monument, Image}

object ListFiller {

  def fillLists(monumentDb: MonumentDB, imageDb: ImageDB) {
    val task = new ListFillerTask(MwBot.ukWiki, monumentDb, imageDb)
    val updater = new PageUpdater(task)
    updater.update()
  }

  def bestImage(images: Seq[Image]) =
    images.sortBy(image => image.size.get + image.width.get * image.height.get).last

}

class ListFillerTask(val host: String, monumentDb: MonumentDB, imageDb: ImageDB) extends PageUpdateTask {

  val titles = pagesToFill(monumentDb, imageDb)

  val uploadConfig = monumentDb.contest.uploadConfigs.head

  override def updatePage(title: String, pageText: String): (String, String) = {
    val template = uploadConfig.listTemplate
    val splitted = pageText.split("\\{\\{" + template)

    var added: Int = 0
    val edited = Seq(splitted.head) ++ splitted.tail.map { text =>
      val monument = Monument.init("{{" + template + "\n" + text, title, uploadConfig.listConfig).head
      if (monument.photo.isEmpty && imageDb.byId(monument.id).nonEmpty) {
        added += 1
        val image = ListFiller.bestImage(imageDb.byId(monument.id))
        monument.copy(
          photo = Some(image.title.replaceFirst("File:", "").replaceFirst("Файл:", ""))
        ).asWiki.replaceFirst("\\{\\{" + template, "")
      }
      else {
        text
      }
    }

    val newText = edited.mkString("{{" + template)
    val comment = s"adding $added image(s)"
    (newText, comment)
  }

  def pagesToFill(monumentDb: MonumentDB, imageDb: ImageDB): Set[String] = {

    val monumentsToFill = monumentDb.monuments.filter {
      m => m.photo.isEmpty && imageDb.containsId(m.id)
    }

    println(s"NewIds: ${monumentsToFill.size}")

    val titles = monumentsToFill.map(_.page).toSet

    println(s"pages: ${titles.size}")
    titles
  }

}
