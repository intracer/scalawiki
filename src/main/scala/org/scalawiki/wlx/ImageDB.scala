package org.scalawiki.wlx

import org.scalawiki.wlx.dto.{Contest, Image, Monument}
import org.scalawiki.wlx.query.ImageQuery

import scala.concurrent.Future

class ImageDB(val contest: Contest, val images: Seq[Image], val monumentDb: MonumentDB) {

  val withCorrectIds = images.filter(_.monumentId.fold(false)(monumentDb.ids.contains))

  val _byId: Map[String, Seq[Image]] = withCorrectIds.groupBy(_.monumentId.getOrElse(""))
  val _imagesByRegion: Map[String, Seq[Image]] = withCorrectIds.groupBy(im => Monument.getRegionId(im.monumentId))
  val _idsByRegion: Map[String, Set[String]] = ids.groupBy(Monument.getRegionId)

  val _byAuthor: Map[String, Seq[Image]] = withCorrectIds.groupBy(_.author.getOrElse(""))

  val _authorsByRegion: Map[String, Set[String]] = _imagesByRegion.mapValues{
    images => images.groupBy(_.author.getOrElse("")).keySet
  }

  val _authorsIds: Map[String, Set[String]] = _byAuthor.mapValues(images => images.groupBy(_.monumentId.getOrElse("")).keySet)

  val _authorIdsByRegion = _authorsIds.mapValues(ids => ids.groupBy(id => Monument.getRegionId(id)))

  def authorsCountById: Map[String, Int] = _byId.mapValues(_.flatMap(_.author).toSet.size)

  def imageCountById: Map[String, Int] = _byId.mapValues(_.size)

  def byNumberOfAuthors: Map[Int, Map[String, Seq[Image]]] = {
    _byId.toSeq.groupBy {
      case (id, photos) =>
        val authors = photos.flatMap(_.author).toSet
        authors.size
    }.mapValues(_.toMap)
  }

  def byNumberOfPhotos: Map[Int, Map[String, Seq[Image]]] = {
    _byId.toSeq.groupBy {
      case (id, photos) => -photos.size
    }.mapValues(_.toMap)
  }

  def ids: Set[String] = _byId.keySet

  def authors: Set[String] = _byAuthor.keySet

  def byId(id: String) = _byId.getOrElse(id, Seq.empty[Image])

  def imagesByRegion(regId: String) = _imagesByRegion.getOrElse(regId, Seq.empty[Image])

  def idsByRegion(regId: String) = _idsByRegion.getOrElse(regId, Seq.empty[String])

  def authorsByRegion(regId: String) = _authorsByRegion.getOrElse(regId, Seq.empty[String])

  def subSet(monuments: Seq[Monument], withFalseIds: Boolean = false): ImageDB = {
    val subSetMonumentDb = new MonumentDB(contest, monuments, withFalseIds)
    val subSetImages = images.filter(_.monumentId.fold(false)(subSetMonumentDb.ids.contains))
    new ImageDB(contest, subSetImages, subSetMonumentDb)
  }

}

object ImageDB {
  import scala.concurrent.ExecutionContext.Implicits.global

  def create(contest: Contest, imageQuery: ImageQuery, monumentDb: MonumentDB):Future[ImageDB] = {
    imageQuery.imagesFromCategoryAsync(contest.category, contest).map {
      images => new ImageDB(contest, images, monumentDb)
    }
  }
}