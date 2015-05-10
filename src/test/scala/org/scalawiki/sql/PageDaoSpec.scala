package org.scalawiki.sql

import java.sql.SQLException

import org.scalawiki.dto.{Namespace, Page, Revision}
import org.scalawiki.sql.dao.{PageDao, RevisionDao, TextDao}
import org.specs2.mutable.{BeforeAfter, Specification}

import scala.slick.driver.H2Driver
import scala.slick.driver.H2Driver.simple._

class PageDaoSpec extends Specification with BeforeAfter {

  sequential

  implicit var session: Session = _

  val pageDao = new PageDao(H2Driver)
  val textDao = new TextDao(H2Driver)
  val revisionDao = new RevisionDao(H2Driver)

  def createSchema() = MediaWiki.createTables()

  override def before = {
    // session = Database.forURL("jdbc:h2:~/test", driver = "org.h2.Driver").createSession()
    session = Database.forURL("jdbc:h2:mem:test", driver = "org.h2.Driver").createSession()
  }

  override def after = session.close()

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

    "insert with the same id should fail" in { // maybe it should, bit now just skips saving
      createSchema()

      val text = "revision text"
      val revision = Revision.one(text).copy(revId = Some(12))

      val page = Page(None, 0, "title", Seq(revision))

      val pageId = pageDao.insert(page)

      val dbPage = pageDao.withText(pageId.get).get

      pageDao.insert(dbPage)

      pageDao.list.size === 1
      revisionDao.list.size === 1
      textDao.list.size === 1
    }

    "insert with the same title should fail" in {
      createSchema()

      val text = "revision text"
      val revision = Revision.one(text)

      val page1 = Page(None, 0, "title", Seq(revision))
      val page2 = page1.copy()

      val pageId = pageDao.insert(page1)

      pageDao.insert(page2) must throwA[SQLException]

      pageDao.list.size === 1
      revisionDao.list.size === 1
      textDao.list.size === 1
    }

    "insert with the same title different namespace is possible" in {
      createSchema()

      val title = "title"
      val (text1, text2) = ("text1", "text2")

      val page1 = Page(None, Namespace.MAIN, title, Seq(Revision.one(text1)))
      val page2 = Page(None, Namespace.FILE, title, Seq(Revision.one(text2)))

      val pageId1 = pageDao.insert(page1)
      val pageId2 = pageDao.insert(page2)

      val dbPage1 = pageDao.withText(pageId1.get).get
      val dbPage2 = pageDao.withText(pageId2.get).get

      dbPage1.title === "title"
      dbPage2.title === "title"

      dbPage1.text === Some(text1)
      dbPage2.text === Some(text2)
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

}
