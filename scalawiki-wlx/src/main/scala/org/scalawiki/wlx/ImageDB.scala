package org.scalawiki.wlx

import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.scalawiki.wlx.query.ImageQuery

import scala.concurrent.Future

class ImageDB(val contest: Contest, val images: Seq[Image],
              val monumentDb: Option[MonumentDB],
              val oldMonumentDb: Option[MonumentDB] = None) {

  def this(contest: Contest, images: Seq[Image]) = this(contest, images, None)

  def this(contest: Contest, images: Seq[Image], monumentDb: MonumentDB) = this(contest, images, Some(monumentDb))

  val withCorrectIds = monumentDb.fold(images)(db => images.filter(_.monumentId.fold(false)(db.ids.contains)))

  val _byId: Map[String, Seq[Image]] = withCorrectIds.groupBy(_.monumentId.getOrElse(""))
  val _imagesByRegion: Map[String, Seq[Image]] = withCorrectIds.groupBy(im => Monument.getRegionId(im.monumentId))
  val _idsByRegion: Map[String, Set[String]] = ids.groupBy(Monument.getRegionId)

  val _byAuthor: Map[String, Seq[Image]] = withCorrectIds.groupBy(_.author.getOrElse(""))

  val _authorsByRegion: Map[String, Set[String]] = _imagesByRegion.mapValues {
    images => images.groupBy(_.author.getOrElse("")).keySet
  }

  val _authorsIds: Map[String, Set[String]] = _byAuthor.mapValues(images => images.groupBy(_.monumentId.getOrElse("")).keySet)

  val _authorIdsByRegion = _authorsIds.mapValues(ids => ids.groupBy(id => Monument.getRegionId(id)))

  val _byMegaPixels: Map[Option[Int], Seq[Image]] = images.groupBy { i =>
    i.pixels.map(px => Math.floor(px.toFloat / Math.pow(10, 6)).toInt)
  }

  val _byMegaPixelsAndAuthor = _byMegaPixels.mapValues {
    images => images.groupBy(_.author.getOrElse(""))
  }

  def byMegaPixels(mp: Option[Int]): Seq[Image] = _byMegaPixels.getOrElse(mp, Seq.empty[Image])

  def byMegaPixelFilterAuthorMap(predicate: (Int => Boolean)) = {
    val values = _byMegaPixelsAndAuthor.filterKeys(_.exists(predicate)).values
    values.fold(Map.empty)((m1, m2) => unionWith(m1, m2)(_ ++ _))
  }

  def unionWithKey[K, A](m1: Map[K, A], m2: Map[K, A])(f: (K, A, A) => A): Map[K, A] = {
    val diff = m2 -- m1.keySet
    val aug = m1 map {
      case (k, v) => if (m2 contains k) k -> f(k, v, m2(k)) else (k, v)
    }
    aug ++ diff
  }

  def unionWith[K, A](m1: Map[K, A], m2: Map[K, A])(f: (A, A) => A): Map[K, A] =
    unionWithKey(m1, m2)((_, x, y) => f(x, y))

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

  def containsId(id: String) = _byId.contains(id)

  def imagesByRegion(regId: String) = _imagesByRegion.getOrElse(regId, Seq.empty[Image])

  def idsByRegion(regId: String) = _idsByRegion.getOrElse(regId, Seq.empty[String])

  def authorsByRegion(regId: String) = _authorsByRegion.getOrElse(regId, Seq.empty[String])

  def subSet(monuments: Seq[Monument], withFalseIds: Boolean = false): ImageDB = {
    val subSetMonumentDb = new MonumentDB(monumentDb.get.contest, monuments, withFalseIds)
    val subSetImages = images.filter(_.monumentId.fold(false)(subSetMonumentDb.ids.contains))
    new ImageDB(contest, subSetImages, Some(subSetMonumentDb))
  }

}

object ImageDB {

  import scala.concurrent.ExecutionContext.Implicits.global

  def create(contest: Contest, imageQuery: ImageQuery, monumentDb: Option[MonumentDB], oldMonumentDb: Option[MonumentDB] = None): Future[ImageDB] = {
    imageQuery.imagesFromCategoryAsync(contest.category, contest).map {
      images => new ImageDB(contest, images, monumentDb, oldMonumentDb)
    }
  }
}