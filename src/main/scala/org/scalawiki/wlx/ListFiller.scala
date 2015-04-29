package org.scalawiki.wlx

import org.scalawiki.wlx.dto.{Image, Monument}
import org.scalawiki.{MwBot, WithBot}

import scala.collection.immutable.SortedSet

class ListFiller extends WithBot {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent._

  def host = MwBot.ukWiki

  def fillLists(monumentDb: MonumentDB, imageDb: ImageDB) {

    val monumentsWithoutImages = monumentDb.monuments.filter(_.photo.isEmpty)
    val idsToFill = monumentsWithoutImages.map(_.id).toSet

    val newIds = imageDb.ids.intersect(idsToFill)

    println(s"NewIds: ${newIds.size}")

    val monumentToFill = monumentsWithoutImages.filter(m => newIds.contains(m.id))
    val monumentsByPage = monumentToFill.groupBy(_.containingPage)

    val titles = SortedSet(monumentsByPage.keys.toSeq: _*)
    println(s"pages: ${titles.size}")

    //val bot = MwBot.get()
    //val javaBot = bot.getJavaWiki
    val results = titles.zipWithIndex.map {
      case (title, index) =>
      println(s"adding monuments to page: $title, $index of ${titles.size}")
      val ids = monumentsByPage(title).map(_.id).toSet

      val result = bot.pageText(title).flatMap { pageText =>
          val (newText: String, comment: String) = addPhotosToPageText(monumentDb, imageDb, titles, title, index, ids, pageText)
          bot.page(title).edit(newText, comment)
      }
      result
    }

   for (seq <- Future.sequence(results.toSeq)) {
     val succesfull = seq.count(_ == "Success")
     println (s"Succesfull pages inserts: $succesfull")

     val errors = seq.filterNot(_ == "Success")

     println (s"Errors in  page inserts: ${errors.size}")
     errors.foreach(println)
   }
  }

  def addPhotosToPageText(monumentDb: MonumentDB, imageDb: ImageDB, titles: SortedSet[String], title: String, index: Int, ids: Set[String], pageText: String): (String, String) = {
    println(s"got page text: $title, $index of ${titles.size}")
    val splitted = pageText.split("\\{\\{" + monumentDb.contest.listTemplate)

    val edited = splitted.zipWithIndex.map { case (text, index) =>
      if (index == 0)
        text
      else {
        val monument = Monument.init(text, title, monumentDb.contest.uploadConfigs.head.listConfig.namesMap)

        if (!ids.contains(monument.id))
          text
        else {
          monument.setTemplateParam("фото", " " + bestImage(imageDb.byId(monument.id)).title).text.replaceFirst("File:", "")
        }
      }
    }

    val newText = edited.mkString("{{" + monumentDb.contest.listTemplate)
    val comment = s"adding ${ids.size} image(s)"
    (newText, comment)
  }

  def bestImage(images: Seq[Image]) =
    images.sortBy(image => image.size + image.width * image.height).last


}
