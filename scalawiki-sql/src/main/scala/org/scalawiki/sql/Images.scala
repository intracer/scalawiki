package org.scalawiki.sql

import org.scalawiki.dto.{Contributor, User}
import org.scalawiki.dto.Image

import slick.driver.H2Driver.api._

/** https://www.mediawiki.org/wiki/Manual:Image_table The image table describes
  * images and other uploaded files. However, the image description pages are
  * stored like other pages.
  * @param tag
  */
class Images(tag: Tag, tableName: String) extends Table[Image](tag, tableName) {

  /** Filename using underscores.
    * @return
    */
  def name = column[String]("img_name", O.PrimaryKey)

  /** File size in bytes.
    * @return
    */
  def size = column[Long]("img_size")

  /** Image width, in pixels.
    * @return
    */
  def width = column[Int]("img_width")

  /** Image height, in pixels.
    * @return
    */
  def height = column[Int]("img_height")

  /** Serialized PHP array of the file's properties.
    * @return
    */
  def metadata = column[String]("img_metadata")

  /** Bit-depth of GIF/PNG palette-based images (up to 8-bit). Non-palette
    * images (JPEG/PNG/TIFF/SVG) are 0, 8, or 16. All other files default to 0.
    * @return
    */
  def bits = column[Int]("img_bits")

  /** Possibilities are UNKNOWN, BITMAP, DRAWING, AUDIO, VIDEO, MULTIMEDIA,
    * OFFICE, TEXT, EXECUTABLE, and ARCHIVE.
    * @return
    */
  def mediaType = column[String]("img_media_type")

  /** Possibilities are unknown, application, audio, chemical, image, message,
    * model, multipart, text, and video.
    * @return
    */
  def majorMime = column[String]("img_major_mime")

  /** E.g. jpeg, gif, png, etc.
    * @return
    */
  def minorMime = column[String]("img_minor_mime")

  /** Description field given during upload. It's not the description page
    * (associated File: wiki page), but the "summary" provided by the user in
    * case of reupload.
    * @return
    */
  def description = column[String]("img_description")

  /** User ID of who uploaded the file.
    * @return
    */
  def userId = column[Long]("img_user")

  /** User name of who uploaded the file.
    * @return
    */
  def userText = column[String]("img_user_text")

  /** Timestamp of when upload took place. Not necessarily the same timestamp as
    * logging.log_timestamp. (?)
    * @return
    */
  def timestamp = column[String]("img_timestamp")

  /** The SHA-1 hash of the file contents in base 36 format.
    * @return
    */
  def sha1 = column[String]("img_sha1")

  def url = column[Option[String]]("img_url")

  def author = column[Option[String]]("img_author")

  def pageId = column[Option[Long]]("img_page_id")

  def * = (
    name,
    size,
    width,
    height,
    url,
    userId,
    userText,
    author,
    pageId
  ) <> (fromDb, toDb)

  def fromDb(
      t: (
          String,
          Long,
          Int,
          Int,
          Option[String],
          Long,
          String,
          Option[String],
          Option[Long]
      )
  ) =
    Images.fromDb(t)

  def toDb(i: Image) = Some(
    (
      i.title,
      i.size.getOrElse(0L),
      i.width.getOrElse(0),
      i.height.getOrElse(0),
      i.url,
      i.uploader.flatMap(_.id).getOrElse(0L),
      i.uploader.flatMap(_.name).getOrElse(""),
      i.author,
      i.pageId
    )
  )

}

object Images {

  def fromDb(
      t: (
          String,
          Long,
          Int,
          Int,
          Option[String],
          Long,
          String,
          Option[String],
          Option[Long]
      )
  ) =
    new Image(
      title = t._1,
      size = Some(t._2),
      width = Some(t._3),
      height = Some(t._4),
      url = t._5,
      uploader = Contributor(Option(t._6), Option(t._7)).collect {
        case u: User => u
      },
      author = t._8,
      pageId = t._9
    )

  def fromDbJoin(
      t: (
          Option[String],
          Option[Long],
          Option[Int],
          Option[Int],
          Option[String],
          Option[Long],
          Option[String],
          Option[String],
          Option[Long]
      )
  ): Option[Image] =
    t._1.map { title =>
      new Image(
        title = title,
        size = t._2,
        width = t._3,
        height = t._4,
        url = t._5,
        uploader = Contributor(t._6, t._7).collect { case u: User => u },
        author = t._8,
        pageId = t._9
      )
    }

}
