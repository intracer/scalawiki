package org.scalawiki.wlx

class ImageFiller(val monumentDb: MonumentDB, imageDB: ImageDB) {

  val withoutPhoto = monumentDb.monuments.filter(_.photo.isDefined).map(_.id)

  imageDB.ids

}
