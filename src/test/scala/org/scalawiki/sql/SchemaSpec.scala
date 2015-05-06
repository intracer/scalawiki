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

      val withRevisions = pageDao.withRevisions(pageId.get).get
      withRevisions.text === Some(texts.head)

      val allRevisions = withRevisions.revisions
      allRevisions.size === 2

      allRevisions.flatMap(_.content.toSeq) === texts
    }

    "find pages by id" in {
      createSchema()

      val names = Seq("page1", "page2", "page3")
      val texts = Seq("page1Text", "page2Text", "page3Text")
      //      val revisions = texts.map(Revision.one)
      val pages = names.zip(texts).map {
        case (n, t) => Page(None, 0, n, Seq(Revision.one(t)))
      }

      val pageIds = pages.flatMap(p => pageDao.insert(p))
      pageIds.size === 3

      val dbPages = pageDao.find(Set(pageIds.head, pageIds.last))
      dbPages.size === 2
      dbPages.map(_.title) === Seq("page1", "page3")
    }

    "find pages with text by id" in {
      createSchema()

      val names = Seq("page1", "page2", "page3")
      val texts = Seq("page1Text", "page2Text", "page3Text")
      //      val revisions = texts.map(Revision.one)
      val pages = names.zip(texts).map {
        case (n, t) => Page(None, 0, n, Seq(Revision.one(t)))
      }

      val pageIds = pages.flatMap(p => pageDao.insert(p))
      pageIds.size === 3

      val dbPages = pageDao.findWithText(Set(pageIds.head, pageIds.last))
      dbPages.size === 2
      dbPages.map(_.title) === Seq("page1", "page3")
      dbPages.flatMap(_.text.toSeq) === Seq("page1Text", "page3Text")
    }

    "find pages with text by id" in {
      createSchema()

      val names = Seq("page1", "page2", "page3")
      val texts = Seq("page1Text", "page2Text", "page3Text")
      //      val revisions = texts.map(Revision.one)
      val pages = names.zip(texts).map {
        case (n, t) => Page(None, 0, n, Seq(Revision.one(t)))
      }

      val pageIds = pages.flatMap(p => pageDao.insert(p))
      pageIds.size === 3

      val dbPages = pageDao.findWithText(Set(pageIds.head, pageIds.last))
      dbPages.size === 2
      dbPages.map(_.title) === Seq("page1", "page3")
      dbPages.flatMap(_.text.toSeq) === Seq("page1Text", "page3Text")
    }

    "find pages by revIds" in {
      createSchema()

      val names = Seq("page1", "page2", "page3")
      val texts = Seq(
        Seq("page1Text2", "page1Text1"),
        Seq("page2Text2", "page2Text1"),
        Seq("page3Text2", "page3Text1")
      )
      //      val revisions = texts.map(Revision.one)
      val pages = names.zip(texts).map {
        case (n, t) => Page(None, 0, n, t.map(Revision.one))
      }

      val pageIds = pages.flatMap(p => pageDao.insert(p).toSeq)
      pageIds.size === 3

      val withRevisions = pageIds.flatMap(id => pageDao.withRevisions(id))

      withRevisions.map(_.revisions.size) === Seq(2, 2, 2)

      val revIds = withRevisions.flatMap(_.revisions.last.id)

      val dbPages = pageDao.findByRevIds(Set(pageIds.head, pageIds.last), Set(revIds.head, revIds.last))
      dbPages.size === 2
      dbPages.map(_.title) === Seq("page1", "page3")
      dbPages.flatMap(_.text.toSeq) === Seq("page1Text1", "page3Text1")
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
