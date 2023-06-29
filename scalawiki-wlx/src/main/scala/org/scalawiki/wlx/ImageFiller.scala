package org.scalawiki.wlx

import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.Monument

object ImageFiller {

  def fillLists(monumentDb: MonumentDB, imageDb: ImageDB): Unit = {
    ListUpdater.updateLists(monumentDb, new ImageFillerUpdater(imageDb))
  }

  def bestImage(images: Seq[Image]): Image =
    images.maxBy { image =>
      (for {
        size <- image.size
        width <- image.width
        height <- image.height
      } yield size + width * height).getOrElse(0L)
    }
}

class ImageFillerUpdater(imageDb: ImageDB) extends MonumentUpdater {

  val nameParam =
    imageDb.contest.uploadConfigs.head.listConfig.namesMap("photo")

  def updatedParams(monument: Monument): Map[String, String] = {
    val image = ImageFiller.bestImage(imageDb.byId(monument.id))
    val value = image.title.replaceFirst("File:", "").replaceFirst("Файл:", "")
    Map(nameParam -> value)
  }

  def needsUpdate(m: Monument): Boolean =
    m.photo.isEmpty && imageDb.containsId(m.id)
}
