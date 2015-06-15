package org.scalawiki.sql

import java.sql.SQLException

import org.scalawiki.dto.User
import org.scalawiki.wlx.dto.Image
import org.specs2.mutable.{BeforeAfter, Specification}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

class ImageDaoSpec extends Specification with BeforeAfter {

  sequential

  var mwDb: MwDatabase = _

  val imageDao = mwDb.imageDao

  def createSchema() = {
    mwDb.dropTables()
    mwDb.createTables()
  }

  override def before = {
    val dc = DatabaseConfig.forConfig[JdbcProfile]("h2mem")
    mwDb = new MwDatabase(dc.db)
  }

  override def after = {
    //mwDb.db.close()
  }

  "image" should {
    "insert" in {
      createSchema()

      val emptyUser = User(0, "")
      val title = "Image.jpg"
      val image = new Image(
        title,
        Some("http://Image.jpg"), None,
        Some(1000 * 1000), Some(800), Some(600)
      )

      imageDao.insert(image)

      imageDao.list.size === 1
      imageDao.get(title) === Some(image.copy(uploader = Some(emptyUser)))
    }

    "insert with user" in {
      createSchema()

      val user = User(5, "username")
      val title = "Image.jpg"
      val image = new Image(
        title,
        Some("http://Image.jpg"), None,
        Some(1000 * 1000), Some(800), Some(600),
        user.name, Some(user)
      )

      imageDao.insert(image)
      imageDao.list.size === 1
      imageDao.get(title) === Some(image)
    }

    "insert with the same title should fail" in {
      createSchema()

      val emptyUser = User(0, "")
      val title = "Image.jpg"
      val image = new Image(
        title,
        Some("http://Image.jpg"), None,
        Some(1000 * 1000), Some(800), Some(600)
      )
      val image2 = image.copy()

      imageDao.insert(image)
      imageDao.insert(image2) must throwA[SQLException]

      imageDao.list.size === 1
    }
  }
}
