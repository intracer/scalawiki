package org.scalawiki.sql

import org.scalawiki.dto.User
import org.scalawiki.sql.dao.ImageDao
import org.scalawiki.wlx.dto.Image
import org.specs2.mutable.{BeforeAfter, Specification}

import scala.slick.driver.H2Driver
import scala.slick.driver.H2Driver.simple._

class ImageDaoSpec extends Specification with BeforeAfter {

  sequential

  implicit var session: Session = _

  val imageDao = new ImageDao(H2Driver)

  def createSchema() = MediaWiki.createTables()

  override def before = {
    // session = Database.forURL("jdbc:h2:~/test", driver = "org.h2.Driver").createSession()
    session = Database.forURL("jdbc:h2:mem:test", driver = "org.h2.Driver").createSession()
  }

  override def after = session.close()

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
