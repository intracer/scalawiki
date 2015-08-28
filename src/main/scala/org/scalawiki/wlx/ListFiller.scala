package org.scalawiki.wlx

import org.scalawiki.wlx.dto.{Image, Monument, UploadConfig}
import org.scalawiki.{MwBot, WithBot}

class ListFiller extends WithBot {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent._

  def host = MwBot.ukWiki

  def fillLists(monumentDb: MonumentDB, imageDb: ImageDB) {

    val titles = pagesToFill(monumentDb, imageDb)

    val uploadConfig = monumentDb.contest.uploadConfigs.head

    val results = titles.zipWithIndex.map {
      case (title, index) =>
        println(s"adding monuments to page: $title, $index of ${titles.size}")
        addPhotosToPage(uploadConfig, imageDb, title)
    }

    for (seq <- Future.sequence(results.toSeq)) {
      val (succesfull, errors) = seq.partition(_ == "Success")

      println(s"Succesfull pages inserts: ${succesfull.size}")
      println(s"Errors in  page inserts: ${errors.size}")

      errors.foreach(println)
    }
  }

  def addPhotosToPage(uploadConfig: UploadConfig, imageDb: ImageDB, title: String): Future[Any] = {
    bot.pageText(title).flatMap { pageText =>
      val (newText: String, comment: String) = addPhotosToPageText(uploadConfig, imageDb, title, pageText)
      bot.page(title).edit(newText, Some(comment))
    }
  }

  def pagesToFill(monumentDb: MonumentDB, imageDb: ImageDB): Set[String] = {

    val monumentsToFill = monumentDb.monuments.filter{
      m => m.photo.isEmpty && imageDb.containsId(m.id)
    }

    println(s"NewIds: ${monumentsToFill.size}")

    val titles = monumentsToFill.map(_.page).toSet

    println(s"pages: ${titles.size}")
    titles
  }

  def addPhotosToPageText(uploadConfig: UploadConfig, imageDb: ImageDB, title: String, pageText: String): (String, String) = {
    val template = uploadConfig.listTemplate
    val splitted = pageText.split("\\{\\{" + template)

    var added: Int = 0
    val edited = Seq(splitted.head) ++ splitted.tail.map { text =>
      val monument = Monument.init("{{" + template + "\n" + text, title, uploadConfig.listConfig).head
      if (monument.photo.isEmpty && imageDb.byId(monument.id).nonEmpty) {
        added += 1
        val image = bestImage(imageDb.byId(monument.id))
        monument.copy(
          photo = Some(image.title.replaceFirst("File:", ""))
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

  def bestImage(images: Seq[Image]) =
    images.sortBy(image => image.size.get + image.width.get * image.height.get).last

}
