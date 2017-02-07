package org.scalawiki.wlx

import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.scalawiki.wlx.query.ImageQuery

import scala.concurrent.Future


class ImageDB(val contest: Contest,
              val images: Seq[Image],
              val monumentDb: Option[MonumentDB]) {

  def this(contest: Contest, images: Seq[Image]) = this(contest, images, None)

  def this(contest: Contest, images: Seq[Image], monumentDb: MonumentDB) = this(contest, images, Some(monumentDb))

  // images
  val _byMegaPixels: Grouping[Int, Image] = new Grouping("mpx", ImageGrouping.byMpx, images)

  val withCorrectIds: Seq[Image] = monumentDb.fold(images) {
    db => images.filter(_.monumentId.exists(db.ids.contains))
  }

  val _byId: Grouping[String, Image] = new Grouping("monument", ImageGrouping.byMonument, withCorrectIds)

  val _byRegion: Grouping[String, Image] = new Grouping("monument", ImageGrouping.byRegion, withCorrectIds)

  val _byAuthor: Grouping[String, Image] = new Grouping("author", ImageGrouping.byAuthor, withCorrectIds)

  val _byRegionAndId: NestedGrouping[String, Image] = _byRegion.compose(ImageGrouping.byMonument)

  val _byRegionAndAuthor: NestedGrouping[String, Image] = _byRegion.compose(ImageGrouping.byAuthor)

  val _byAuthorAndId: NestedGrouping[String, Image] = _byAuthor.compose(ImageGrouping.byMonument)

  val _byAuthorAndRegion: NestedGrouping[String, Image] = _byAuthor.compose(ImageGrouping.byRegion)

  val _byMegaPixelsAndAuthor = _byMegaPixels.grouped.mapValues {
    images => images.groupBy(_.author.getOrElse(""))
  }

  def byMegaPixels(mp: Int): Seq[Image] = _byMegaPixels.by(mp)

  def ids: Set[String] = _byId.keys

  def authors: Set[String] = _byAuthor.keys

  def byId(id: String) = _byId.by(id)

  def containsId(id: String) = _byId.contains(id)

  def imagesByRegion(regId: String) = _byRegion.by(regId)

  def idsByRegion(regId: String) = _byRegionAndId.by(regId).keys

  def authorsByRegion(regId: String) = _byRegionAndAuthor.by(regId).keys

  def byMegaPixelFilterAuthorMap(predicate: (Int => Boolean)): Map[String, Seq[Image]] = {
    _byMegaPixels.grouped.filterKeys(mpx => mpx >= 0 && predicate(mpx)).values.flatten.toSeq.groupBy(_.author.getOrElse(""))
  }

  def authorsCountById: Map[String, Int] = _byId.grouped.mapValues(_.flatMap(_.author).toSet.size)

  def imageCountById: Map[String, Int] = _byId.grouped.mapValues(_.size)

  def byNumberOfAuthors: Map[Int, Map[String, Seq[Image]]] = {
    _byId.grouped.groupBy {
      case (id, photos) =>
        photos.flatMap(_.author).toSet.size
    }.mapValues(_.toMap)
  }

  def byNumberOfPhotos: Map[Int, Map[String, Seq[Image]]] = {
    _byId.grouped.groupBy {
      case (id, photos) => photos.size
    }.mapValues(_.toMap)
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
      grouped.mapValues(v => new Grouping("", g, v))
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

  def create(contest: Contest, imageQuery: ImageQuery, monumentDb: Option[MonumentDB]): Future[ImageDB] = {
    imageQuery.imagesFromCategoryAsync(contest.category, contest).map {
      images => new ImageDB(contest, images, monumentDb)
    }
  }
}