package org.scalawiki.sql


import org.scalawiki.dto.{User, Page, Revision}
import org.scalawiki.sql.dao.{UserDao, PageDao, RevisionDao, TextDao}
import org.specs2.mutable.{BeforeAfter, Specification}

import scala.slick.driver.H2Driver
import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable

class SchemaSpec extends Specification with BeforeAfter {

  sequential

  implicit var session: Session = _

  val pageDao = new PageDao(H2Driver)
  val textDao = new TextDao(H2Driver)
  val revisionDao = new RevisionDao(H2Driver)
  val userDao = new UserDao(H2Driver)

  def createSchema() = MediaWiki.createTables()

  override def before = {
    // session = Database.forURL("jdbc:h2:~/test", driver = "org.h2.Driver").createSession()
    session = Database.forURL("jdbc:h2:mem:test", driver = "org.h2.Driver").createSession()
  }

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

  "page" should {
    "not insert without revision" in {
      createSchema()

      val page = new Page(None, 0, "title")
      pageDao.insert(page) must throwA[IllegalArgumentException]

    }

    "insert with revision" in {
      createSchema()

      val text = "revision text"
      val revision = Revision.one(text)

      val page = Page(None, 0, "title", Seq(revision))

      val pageId = pageDao.insert(page)

      val dbPage = pageDao.withText(pageId.get).get

      dbPage.text === Some(text)

      dbPage.revisions.size === 1
    }

    "insert with two revisions" in {
      createSchema()

      val texts = Seq("rev2", "rev1")
      val revisions = texts.map(Revision.one)

      val page = Page(None, 0, "title", revisions)

      val pageId = pageDao.insert(page)
      pageId.isDefined === true

      val dbPage = pageDao.withText(pageId.get).get

      dbPage.text === Some(texts.head)

      dbPage.revisions.size === 1
      val lastRevision = dbPage.revisions.head

      val withRevisions = pageDao.withRevisions(pageId.get).get

      withRevisions.text === Some(texts.head)

      val allRevisions = withRevisions.revisions
      allRevisions.size === 2

      allRevisions.flatMap(_.content.toSeq) === texts
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


  override def after = session.close()
}
