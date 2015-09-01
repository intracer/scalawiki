package org.scalawiki.wlx

import org.scalawiki.MwBot
import org.scalawiki.edit.{PageUpdateTask, PageUpdater}
import org.scalawiki.wlx.dto.Monument

object ListFileNSRemover {

  def updateLists(monumentDb: MonumentDB) {
    val task = new ListFileNSRemoverTask(MwBot.ukWiki, monumentDb)
    val updater = new PageUpdater(task)
    updater.update()
  }
}

class ListFileNSRemoverTask(val host: String, monumentDb: MonumentDB) extends PageUpdateTask {

  val titles = pagesToFix(monumentDb)

  val uploadConfig = monumentDb.contest.uploadConfigs.head

  override def updatePage(title: String, pageText: String): (String, String) = {
    val template = uploadConfig.listTemplate
    val splitted = pageText.split("\\{\\{" + template)

    var added: Int = 0
    val edited = Seq(splitted.head) ++ splitted.tail.map { text =>
      val monument = Monument.init("{{" + template + "\n" + text, title, uploadConfig.listConfig).head
      if (needsUpdate(monument)) {
        added += 1
        val photo = monument.photo.get.replaceFirst("File:", "").replaceFirst("Файл:", "")
        monument.copy(
          photo = Some(photo)
        ).asWiki.replaceFirst("\\{\\{" + template, "")
      }
      else {
        text
      }
    }

    val newText = edited.mkString("{{" + template)
    val comment = s"fixing $added image(s)"
    (newText, comment)
  }

  def needsUpdate(m: Monument): Boolean =
    m.photo.exists(photo => photo.trim.startsWith("File:") || photo.trim.startsWith("Файл:"))

  def pagesToFix(monumentDb: MonumentDB): Set[String] = {

    val monumentsToFill = monumentDb.monuments.filter(needsUpdate)

    println(s"Ids: ${monumentsToFill.size}")

    val titles = monumentsToFill.map(_.page).toSet

    println(s"pages: ${titles.size}")
    titles
  }

}
