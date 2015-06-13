package org.scalawiki.sql

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

      val emptyUser = User(Some(0), Some(""))
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

      val user = User(Some(5), Some("username"))
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
  }
}
