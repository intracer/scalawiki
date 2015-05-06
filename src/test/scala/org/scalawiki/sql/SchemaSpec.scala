package org.scalawiki.sql


import org.scalawiki.dto.User
import org.scalawiki.sql.dao.UserDao
import org.specs2.mutable.{BeforeAfter, Specification}

import scala.slick.driver.H2Driver
import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable

class SchemaSpec extends Specification with BeforeAfter {

  sequential

  implicit var session: Session = _

  val userDao = new UserDao(H2Driver)

  def createSchema() = MediaWiki.createTables()

  override def before = {
    // session = Database.forURL("jdbc:h2:~/test", driver = "org.h2.Driver").createSession()
    session = Database.forURL("jdbc:h2:mem:test", driver = "org.h2.Driver").createSession()
  }

  override def after = session.close()

  "ddls" should {
    "create schema" in {
      createSchema()

      val tables = MTable.getTables.list
      val tableNames = tables.map(_.name.name.toLowerCase).toSet

      tableNames === Set("category",
        "image",
        "page",
        "revision",
        "text",
        "user")
    }
  }

  "user" should {
    "insert" in {
      createSchema()

      val user = User(None, Some("Username"))
      val userId = userDao.insert(user)

      val users = userDao.list

      users.size === 1
      val dbUser = users.head

      dbUser === user.copy(id = userId)
    }
  }


}
