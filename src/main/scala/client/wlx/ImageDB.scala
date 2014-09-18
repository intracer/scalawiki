package client.wlx

import client.wlx.dto.{Monument, Contest, Image}

class ImageDB(val contest: Contest, val images: Seq[Image], val monumentDb: MonumentDB) {

  val withCorrectIds = images.filter(_.monumentId.fold(false)(monumentDb.ids.contains))

  val _byId: Map[String, Seq[Image]] = withCorrectIds.groupBy(_.monumentId.getOrElse(""))
  val _imagesByRegion: Map[String, Seq[Image]] = withCorrectIds.groupBy(m => Monument.getRegionId(m.monumentId))
  val _idsByRegion: Map[String, Set[String]] = ids.groupBy(Monument.getRegionId)

  val _byAuthor: Map[String, Seq[Image]] = withCorrectIds.groupBy(_.author.getOrElse(""))

  val _authorsByRegion: Map[String, Set[String]] = _imagesByRegion.mapValues{
    images => images.groupBy(_.author.getOrElse("")).keySet
  }

//  var allImages: Seq[Image] = Seq.empty

  def ids: Set[String] = _byId.keySet

  def authors: Set[String] = _byAuthor.keySet

  def byId(id: String) = _byId.getOrElse(id, Seq.empty[Image])

  def imagesByRegion(regId: String) = _imagesByRegion.getOrElse(regId, Seq.empty[Image])

  def idsByRegion(regId: String) = _idsByRegion.getOrElse(regId, Seq.empty[String])

  def authorsByRegion(regId: String) = _authorsByRegion.getOrElse(regId, Seq.empty[String])

}


