package org.scalawiki.wlx

import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.scalawiki.wlx.query.ImageQuery

import scala.concurrent.Future


case class ImageDB(contest: Contest,
                   images: Seq[Image],
                   monumentDb: Option[MonumentDB],
                   minMpx: Float = 0) {

  def this(contest: Contest, images: Seq[Image]) = this(contest, images, None)

  def this(contest: Contest, images: Seq[Image], monumentDb: MonumentDB) = this(contest, images, Some(monumentDb))

  lazy val _byMegaPixels: Grouping[Int, Image] = new Grouping("mpx", ImageGrouping.byMpx, images)

  lazy val withCorrectIds: Seq[Image] = monumentDb.fold(images) {
    db => images.filter(_.monumentId.exists(db.ids.contains))
  }

  lazy val sansIneligible: Seq[Image] = withCorrectIds
    .filterNot(_.categories.contains(s"Ineligible submissions for WLM ${contest.year} in Ukraine"))
    .filter(_.mpx.exists(_ >= minMpx))

  lazy val ineligible: Seq[Image] = withCorrectIds
    .filter(_.categories.contains(s"Ineligible submissions for WLM ${contest.year} in Ukraine"))
    .filter(_.mpx.exists(_ < minMpx))

  lazy val _byId: Grouping[String, Image] = new Grouping("monument", ImageGrouping.byMonument, withCorrectIds)

  lazy val _byRegion: Grouping[String, Image] = new Grouping("monument", ImageGrouping.byRegion, withCorrectIds)

  lazy val _byAuthor: Grouping[String, Image] = new Grouping("author", ImageGrouping.byAuthor, sansIneligible)

  lazy val _byRegionAndId: NestedGrouping[String, Image] = _byRegion.compose(ImageGrouping.byMonument)

  lazy val _byRegionAndAuthor: NestedGrouping[String, Image] = _byRegion.compose(ImageGrouping.byAuthor)

  lazy val _byAuthorAndId: NestedGrouping[String, Image] = _byAuthor.compose(ImageGrouping.byMonument)

  lazy val _byAuthorAndRegion: NestedGrouping[String, Image] = _byAuthor.compose(ImageGrouping.byRegion)

  lazy val _byMegaPixelsAndAuthor = _byMegaPixels.grouped.mapValues {
    images => images.groupBy(_.author.getOrElse(""))
  }

  def byMegaPixels(mp: Int): Seq[Image] = _byMegaPixels.by(mp)

  def ids: Set[String] = _byId.keys

  def authors: Set[String] = _byAuthor.keys

  def byId(id: String) = _byId.by(id)

  def containsId(id: String) = _byId.contains(id)

  def imagesByRegion(regId: String): Seq[Image] =
    if (regId.length == 2) {
      _byRegion.by(regId)
    } else {
      val parentId = regId.substring(0, 2)
      val topImages = _byRegion.by(parentId)
      topImages.filter(_.monumentId.exists(_.replace("-", "").startsWith(regId)))
    }

  def idsByRegion(regId: String): Set[String] = {
    if (regId.length == 2) {
      _byRegionAndId.by(regId).keys
    } else {
      val parentId = regId.substring(0, 2)
      val topKeys = _byRegionAndId.by(parentId).keys
      topKeys.filter(_.replace("-", "").startsWith(regId))
    }
  }

  def idByAuthor(author: String) = _byAuthorAndId.by(author).keys

  def authorsByRegion(regId: String) = _byRegionAndAuthor.by(regId).keys

  def byMegaPixelFilterAuthorMap(predicate: Int => Boolean): Map[String, Seq[Image]] = {
    _byMegaPixels.grouped.filterKeys(mpx => mpx >= 0 && predicate(mpx)).values.flatten.toSeq.groupBy(_.author.getOrElse(""))
  }

  def authorsCountById: Map[String, Int] = _byId.grouped.mapValues(_.flatMap(_.author).toSet.size).toMap

  def imageCountById: Map[String, Int] = _byId.grouped.mapValues(_.size).toMap

  def byNumberOfAuthors: Map[Int, Map[String, Seq[Image]]] = {
    _byId.grouped.groupBy {
      case (id, photos) =>
        photos.flatMap(_.author).toSet.size
    }.mapValues(_.toMap).toMap
  }

  def byNumberOfPhotos: Map[Int, Map[String, Seq[Image]]] = {
    _byId.grouped.groupBy {
      case (id, photos) => photos.size
    }.mapValues(_.toMap).toMap
  }

  def subSet(monuments: Seq[Monument], withFalseIds: Boolean = false): ImageDB = {
    val subSetMonumentDb = new MonumentDB(monumentDb.get.contest, monuments, withFalseIds)
    val subSetImages = images.filter(_.monumentId.fold(false)(subSetMonumentDb.ids.contains))
    new ImageDB(contest, subSetImages, Some(subSetMonumentDb))
  }

  def subSet(f: Image => Boolean): ImageDB = {
    new ImageDB(contest, images.filter(f), monumentDb)
  }

}

class Grouping[T, F](name: String, val f: F => T, data: Seq[F]) {

  val grouped: Map[T, Seq[F]] = data.groupBy(f)

  def by(key: T): Seq[F] = grouped.getOrElse(key, Seq.empty)

  def contains(id: T): Boolean = grouped.contains(id)

  def headBy(key: T): F = by(key).head

  def headOptionBy(key: T): Option[F] = by(key).headOption

  def keys: Set[T] = grouped.keySet

  def size = grouped.size

  def compose(g: F => T): NestedGrouping[T, F] =
    new NestedGrouping(
      grouped.mapValues(v => new Grouping("", g, v)).toMap
    )

}

class NestedGrouping[T, F](val grouped: Map[T, Grouping[T, F]]) {

  val keys: Set[T] = grouped.keySet

  def by(key: T): Grouping[T, F] = grouped.getOrElse(key, new Grouping[T, F]("", null, Seq.empty))

  def by(key1: T, key2: T): Seq[F] = by(key1).by(key2)

  def headBy(key1: T, key2: T): F = by(key1).headBy(key2)

  def headOptionBy(key1: T, key2: T): Option[F] = by(key1).headOptionBy(key2)

}

object ImageGrouping {

  def byMpx = (i: Image) => i.mpx.map(_.toInt).getOrElse(-1)

  def byMonument = (i: Image) => i.monumentId.getOrElse("")

  def byRegion = (i: Image) => Monument.getRegionId(i.monumentId)

  def byAuthor = (i: Image) => i.author.getOrElse("")

}

object ImageDB {

  import scala.concurrent.ExecutionContext.Implicits.global

  def create(contest: Contest, imageQuery: ImageQuery, monumentDb: Option[MonumentDB], minMpx: Float = 0): Future[ImageDB] = {
    imageQuery.imagesFromCategoryAsync(contest.imagesCategory, contest).map { images =>
      new ImageDB(contest, images, monumentDb, minMpx)
    }
  }
}