package org.scalawiki.wlx.slick

import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.dto.lists.EmptyListConfig
import slick.driver.H2Driver.api._

class Monuments(tag: Tag) extends Table[Monument](tag, "MONUMENTS") {

  def id = column[String]("ID")
  def page = column[String]("PAGE")
  def name = column[String]("NAME")
  def photo = column[String]("PHOTO" )
  def gallery = column[String]("GALLERY")

  def * = (id, page, name, photo.?, gallery.?) <> (fromDb, toDb)

  def fromDb(t:(String, String, String, Option[String], Option[String])) =
    Monument(id = t._1, page = t._2, name = t._3, photo = t._4, gallery = t._5, listConfig = Some(EmptyListConfig))

  def toDb(m:Monument) = Some((m.id, m.page, m.name, m.photo, m.gallery))

}


