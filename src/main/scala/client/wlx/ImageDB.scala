package client.wlx

import client.wlx.dto.{Monument, Contest, Image}
import client.wlx.query.ImageQuery

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
  val _authorIdsByRegion =
    _authorsIds.mapValues(ids => ids.groupBy(id => Monument.getRegionId(id)))

    //  var allImages: Seq[Image] = Seq.empty

  def ids: Set[String] = _byId.keySet

  def authors: Set[String] = _byAuthor.keySet

  def byId(id: String) = _byId.getOrElse(id, Seq.empty[Image])

  def imagesByRegion(regId: String) = _imagesByRegion.getOrElse(regId, Seq.empty[Image])

  def idsByRegion(regId: String) = _idsByRegion.getOrElse(regId, Seq.empty[String])

  def authorsByRegion(regId: String) = _authorsByRegion.getOrElse(regId, Seq.empty[String])

  def subSet(monuments: Seq[Monument]): ImageDB = {
    val subSetMonumentDb = new MonumentDB(contest, monuments)
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


//Error:(46, 70) Cannot find an implicit ExecutionContext, either import scala.concurrent.ExecutionContext.Implicits.global or use a custom one
//imageQuery.imagesFromCategoryAsync(contest.category, contest).map{
