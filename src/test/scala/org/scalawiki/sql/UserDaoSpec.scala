package org.scalawiki.sql

import java.sql.SQLException

import org.scalawiki.dto.User
import org.specs2.mutable.{BeforeAfter, Specification}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

class UserDaoSpec extends Specification with BeforeAfter {

  sequential

  var mwDb: MwDatabase = _

  def userDao = mwDb.userDao

  def createSchema() = {
    mwDb.dropTables()
    mwDb.createTables()
  }

  override def before = {
      val dc = DatabaseConfig.forConfig[JdbcProfile]("h2mem")
      mwDb = new MwDatabase(dc.db, None, dc.driver)
  }

  override def after = {
    //mwDb.db.close()
  }

  "user" should {

    "insert" in {
      createSchema()
      val username = Some("username")
      val user = User(None, username)

      val userId = userDao.insert(user).get

      val dbUser = userDao.get(userId).get
      dbUser.login === username
    }

    "insert with id" in {
      createSchema()
      val username = Some("username")
      val user = User(5, username.get)

      val userId = userDao.insert(user).get

      val dbUser = userDao.get(userId).get
      dbUser.id === Some(5)
      dbUser.login === username
    }

    "insert empty user should fail" in {
      createSchema()
      val user = User(None, None)

      userDao.insert(user) must throwA[SQLException]

      userDao.list.isEmpty === true
    }

    "insert user without name should fail" in {
      createSchema()
      val user = new User(Some(6), None)

      userDao.insert(user) must throwA[SQLException]

      userDao.list.isEmpty === true
    }

    "insert with the same id should fail" in {
      createSchema()

      val user = User(None, Some("username"))

      val userId = userDao.insert(user)

      val dbUser = userDao.get(userId.get).get
      dbUser.id.isDefined === true
      dbUser.id === userId

      userDao.insert(dbUser.copy(login = Some("other name"))) must throwA[SQLException]

      userDao.list.size === 1
    }

    "insert with the same name should fail" in {
      createSchema()

      val username = Some("username")
      val user = User(None, username)
      val user2 = user.copy()

      val userId = userDao.insert(user)

      userDao.get(userId.get).isDefined === true

      userDao.insert(user2) must throwA[SQLException]

      userDao.list.size === 1
    }

    "find users by id" in {
      createSchema()

      val names = Seq("user1", "user2", "user3")
      val users = names.map {
        name => User(None, Some(name))
      }

      val userIds = users.flatMap(u => userDao.insert(u))
      userIds.size === 3

      val dbUsers = userDao.find(Set(userIds.head, userIds.last))
      dbUsers.size === 2
      dbUsers.map(_.login.get) === Seq("user1", "user3")
    }
  }
}
