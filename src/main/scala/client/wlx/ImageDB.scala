package client.wlx

import client.wlx.dto.{Contest, Image}
import client.wlx.query.ImageQuery

class ImageDB(val contest: Contest, imageQuery: ImageQuery) {

  var images: Seq[Image] = Seq.empty

  var _byId: Map[String, Seq[Image]] = Map.empty
  var _byRegion: Map[String, Seq[Image]] = Map.empty
  var _idsByRegion: Map[String, Set[String]] = Map.empty

  def ids: Set[String] = _byId.keySet

  def fetchImages() {
    images = imageQuery.imagesFromCategory(contest.category, contest)
    _byId = images.groupBy(_.monumentId.getOrElse(""))
    _byRegion = images.groupBy(_.monumentId.getOrElse("").split("\\-")(0))
    _idsByRegion = ids.groupBy(_.split("\\-")(0))
  }

  def byId(id: String) = _byId.getOrElse(id, Seq.empty[Image])

  def byRegion(regId: String) = images.filter(_.monumentId.fold(false)(_.startsWith(regId)))

}
