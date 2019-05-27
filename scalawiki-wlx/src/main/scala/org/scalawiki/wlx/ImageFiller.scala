package org.scalawiki.wlx

import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.Monument

object ImageFiller {

  def fillLists(monumentDb: MonumentDB, imageDb: ImageDB) {
    ListUpdater.updateLists(monumentDb, new ImageFillerUpdater(imageDb))
  }

  def bestImage(images: Seq[Image]) =
    images.sortBy(image => image.size.get + image.width.get * image.height.get).last
}

class ImageFillerUpdater(imageDb: ImageDB) extends MonumentUpdater {

  val nameParam = imageDb.contest.uploadConfigs.head.listConfig.namesMap("photo")

  def updatedParams(monument: Monument): Map[String, String] = {
    val image = ImageFiller.bestImage(imageDb.byId(monument.id))
    val value = image.title.replaceFirst("File:", "").replaceFirst("Файл:", "")
    Map(nameParam -> value)
  }

  def needsUpdate(m: Monument): Boolean = m.photo.isEmpty && imageDb.containsId(m.id)
}