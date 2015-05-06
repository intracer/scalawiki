package org.scalawiki.wlx.slick

import org.scalawiki.wlx.dto.Image
import scala.slick.driver.H2Driver.simple._

class Images(tag: Tag) extends Table[Image](tag, "IMAGES") {

  def pageId = column[Long]("ID", O.NotNull)
  def title = column[String]("TITLE", O.NotNull)
  def monumentId = column[String]("MONUMENT_ID")
  def author = column[String]("AUTHOR")
  def uploader = column[String]("UPLOADER")
  def date = column[String]("DATE")
  def width = column[Int]("WIDTH")
  def height = column[Int]("HEIGHT")
  def size = column[Long]("SIZE")

  def * = (pageId, title, monumentId.?, author.?, uploader.?, date.?, width, height, size) <> (fromDb, toDb)

  def fromDb(t:(Long, String, Option[String], Option[String], Option[String], Option[String], Int, Int, Long)) =
    Image(
      pageId = t._1,
      title = t._2,
      monumentId = t._3,
      author = t._4,
      uploader = t._5,
      year = t._6,
      width = t._7,
      height = t._8,
      size = t._9,
      url = "",
      pageUrl = ""
      )

  def toDb(im:Image) = Some((
    im.pageId,
    im.title,
    im.monumentId,
    im.author,
    im.uploader,
    im.year,
    im.width,
    im.height,
    im.size
    ))
}

