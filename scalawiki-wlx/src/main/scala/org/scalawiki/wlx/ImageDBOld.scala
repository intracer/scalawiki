package org.scalawiki.wlx

import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.scalawiki.wlx.query.ImageQuery

import scala.concurrent.Future

class ImageDBOld(val contest: Contest, val images: Seq[Image],
              val monumentDb: Option[MonumentDB],
              val oldMonumentDb: Option[MonumentDB] = None) {

  def this(contest: Contest, images: Seq[Image]) = this(contest, images, None)

  def this(contest: Contest, images: Seq[Image], monumentDb: MonumentDB) = this(contest, images, Some(monumentDb))

  // images
  private val _byMegaPixels: Map[Int, Seq[Image]] = images.filter(_.mpx.isDefined).groupBy (i => i.mpx.map(_.toInt).getOrElse(-1))

  private val withCorrectIds = monumentDb.fold(images)(db => images.filter(_.monumentId.fold(false)(db.ids.contains)))

  private val _byId: Map[String, Seq[Image]] = withCorrectIds.groupBy(_.monumentId.getOrElse(""))

  private val _imagesByRegion: Map[String, Seq[Image]] = withCorrectIds.groupBy(im => Monument.getRegionId(im.monumentId))

  private val _idsByRegion: Map[String, Set[String]] = ids.groupBy(Monument.getRegionId)

  val _byAuthor: Map[String, Seq[Image]] = withCorrectIds.groupBy(_.author.getOrElse(""))

  private val _authorsByRegion: Map[String, Set[String]] = _imagesByRegion.mapValues {
    images => images.groupBy(_.author.getOrElse("")).keySet
  }
  val _authorsIds: Map[String, Set[String]] = _byAuthor.mapValues(images => images.groupBy(_.monumentId.getOrElse("")).keySet)

  val _authorIdsByRegion = _authorsIds.mapValues(ids => ids.groupBy(id => Monument.getRegionId(id)))

  def byMegaPixels(mp: Int): Seq[Image] = _byMegaPixels.getOrElse(mp, Seq.empty[Image])

  def ids: Set[String] = _byId.keySet

  def authors: Set[String] = _byAuthor.keySet

  def byId(id: String) = _byId.getOrElse(id, Seq.empty[Image])

  def containsId(id: String) = _byId.contains(id)

  def imagesByRegion(regId: String) = _imagesByRegion.getOrElse(regId, Seq.empty[Image])

  def idsByRegion(regId: String) = _idsByRegion.getOrElse(regId, Seq.empty[String])

  def authorsByRegion(regId: String) = _authorsByRegion.getOrElse(regId, Seq.empty[String])

  def byMegaPixelFilterAuthorMap(predicate: (Int => Boolean)): Map[String, Seq[Image]] = {
    _byMegaPixels.filterKeys(predicate).values.flatten.toSeq.groupBy(_.author.getOrElse(""))
  }

  def authorsCountById: Map[String, Int] = _byId.mapValues(_.flatMap(_.author).toSet.size)

  def imageCountById: Map[String, Int] = _byId.mapValues(_.size)

  def byNumberOfAuthors: Map[Int, Map[String, Seq[Image]]] = {
    _byId.groupBy {
      case (id, photos) =>
        photos.flatMap(_.author).toSet.size
    }.mapValues(_.toMap)
  }

  def byNumberOfPhotos: Map[Int, Map[String, Seq[Image]]] = {
    _byId.groupBy {
      case (id, photos) => photos.size
    }.mapValues(_.toMap)
  }

  def subSet(monuments: Seq[Monument], withFalseIds: Boolean = false): ImageDB = {
    val subSetMonumentDb = new MonumentDB(monumentDb.get.contest, monuments, withFalseIds)
    val subSetImages = images.filter(_.monumentId.fold(false)(subSetMonumentDb.ids.contains))
    new ImageDB(contest, subSetImages, Some(subSetMonumentDb))
  }
}

object ImageDBOld {

  import scala.concurrent.ExecutionContext.Implicits.global

  def create(contest: Contest, imageQuery: ImageQuery, monumentDb: Option[MonumentDB], oldMonumentDb: Option[MonumentDB] = None): Future[ImageDB] = {
    imageQuery.imagesFromCategoryAsync(contest.category, contest).map {
      images => new ImageDB(contest, images, monumentDb, oldMonumentDb)
    }
  }
}