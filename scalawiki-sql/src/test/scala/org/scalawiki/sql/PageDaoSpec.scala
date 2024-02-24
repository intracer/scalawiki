package org.scalawiki.sql

import java.sql.SQLException

import org.scalawiki.dto.{Namespace, Page, Revision, User}
import org.scalawiki.dto.Image
import org.specs2.mutable.{BeforeAfter, Specification}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

class PageDaoSpec extends Specification with BeforeAfter {

  sequential

  var mwDb: MwDatabase = _

  def pageDao = mwDb.pageDao

  def textDao = mwDb.textDao

  def revisionDao = mwDb.revisionDao

  def userDao = mwDb.userDao

  def imageDao = mwDb.imageDao

  def createSchema() = {
    mwDb.dropTables()
    mwDb.createTables()
  }

  override def before = {
    val dc = DatabaseConfig.forConfig[JdbcProfile]("h2mem")
    mwDb = new MwDatabase(dc)
  }

  override def after = {
    // mwDb.db.close()
  }

  "page" should {
    "not insert without revision" in {
      createSchema()

      val page = new Page(None, Some(0), "title")
      pageDao.insert(page) must throwA[IllegalArgumentException]
      pageDao.count aka "page count" must_== 0
      revisionDao.count aka "revision count" must_== 0
    }

    "insert with revision" in {
      createSchema()

      val text = "revision text"
      val revision = Revision.one(text)

      val page = Page(None, Some(0), "title", Seq(revision))

      val pageId = pageDao.insert(page)

      val dbPage = pageDao.withText(pageId)

      dbPage.text === Some(text)

      dbPage.revisions.size === 1
    }

    "insert with given id page id" in {
      createSchema()

      val text = "revision text"
      val revision = Revision.one(text)

      val page = Page(Some(3), Some(0), "title", Seq(revision))

      val pageId = pageDao.insert(page)
      pageId === 3

      val dbPage = pageDao.withText(pageId)

      dbPage.text === Some(text)
      dbPage.id === Some(3)

      dbPage.revisions.size === 1
    }

    "insert with given id page id and revision id" in {
      createSchema()

      val text = "revision text"
      val revision: Revision =
        new Revision(revId = Some(5L), pageId = Some(3L), content = Some(text))

      val page: Page = new Page(Some(3L), Some(0), "title", Seq(revision))

      val pageId = pageDao.insert(page)
      pageId === 3L

      val dbPage = pageDao.withText(pageId)

      dbPage.text === Some(text)
      dbPage.id === Some(3)

      dbPage.revisions.size === 1
      dbPage.revisions.head.revId === Some(5)
    }

    "insert with user" in {
      createSchema()

      val username = Some("username")
      val user = User(5, username.get)
      val revision: Revision =
        new Revision(user = Some(user), content = Some("revision text"))

      val page = Page(None, Some(0), "title", Seq(revision))

      val pageId = pageDao.insert(page)
      val dbPage = pageDao.withText(pageId)

      val dbRev = dbPage.revisions.head
      val contributor = dbRev.user
      contributor.isDefined === true
      val dbUser = contributor.map({ case u: User => u }).get

      dbUser.name === username
      dbUser.id === Some(5)
    }

    "insert fill user by id" in {
      createSchema()

      val username = Some("username")
      val user = User(5, username.get)
      userDao.insert(user)

      val revision: Revision = new Revision(
        user = Some(user.copy(login = None)),
        content = Some("revision text")
      )

      val page = Page(None, Some(0), "title", Seq(revision))

      val pageId = pageDao.insert(page)
      val dbPage = pageDao.withText(pageId)

      val dbRev = dbPage.revisions.head
      val contributor = dbRev.user
      contributor.isDefined === true
      val dbUser = contributor.map({ case u: User => u }).get

      dbUser.name === username
      dbUser.id === Some(5)
    }

    "insert fill user by name" in {
      createSchema()

      val username = Some("username")
      val user = User(5, username.get)
      userDao.insert(user)

      val revision: Revision = new Revision(
        user = Some(user.copy(id = None)),
        content = Some("revision text")
      )

      val page = Page(None, Some(0), "title", Seq(revision))

      val pageId = pageDao.insert(page)
      val dbPage = pageDao.withText(pageId)

      val dbRev = dbPage.revisions.head
      val contributor = dbRev.user
      contributor.isDefined === true
      val dbUser = contributor.map({ case u: User => u }).get

      dbUser.name === username
      dbUser.id === Some(5)
    }

    "insert with new user should fail" in {
      createSchema()

      val username = Some("username")
      val user = new User(None, username)
      val revision: Revision =
        new Revision(user = Some(user), content = Some("revision text"))

      val page = Page(None, Some(0), "title", Seq(revision))

      pageDao.insert(page) must throwA[IllegalArgumentException]

      // TODO
      //      pageDao.count aka "page count" must_== 0
      //      revisionDao.count aka "revision count" must_== 0
      //      userDao.count aka "user count" must_== 0
    }

    "insert with image" in {
      createSchema()

      val user = User(5, "username")
      val title = "Image.jpg"
      val image = new Image(
        title,
        Some("http://Image.jpg"),
        None,
        Some(1000 * 1000),
        Some(800),
        Some(600),
        user.name,
        Some(user)
      )
      val revision: Revision =
        new Revision(user = Some(user), content = Some("revision text"))

      val page = Page(None, Some(0), title, Seq(revision), Seq(image))

      val pageId = pageDao.insert(page).toOption

      val images = imageDao.list
      images.size === 1
      images.head === image.copy(pageId = pageId)

      val dbImage = imageDao.get(title)
      dbImage === Some(image.copy(pageId = pageId))
    }

    "insert with the same id should fail" in {
      // maybe it should, bit now just skips saving
      createSchema()

      val text = "revision text"
      val revision = Revision.one(text).copy(revId = Some(12))

      val page = Page(None, Some(0), "title", Seq(revision))

      val pageId = pageDao.insert(page)

      val dbPage = pageDao.withText(pageId)
      dbPage.id.isDefined === true
      dbPage.id === Some(pageId)

      pageDao.insert(dbPage.copy(title = "other title"))

      pageDao.list.size === 1
      revisionDao.list.size === 1
      textDao.list.size === 1
    }

    "insert with the same title should fail" in {
      createSchema()

      val text = "revision text"
      val revision = Revision.one(text)

      val page1 = Page(None, Some(0), "title", Seq(revision))
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

      val page1 =
        Page(None, Some(Namespace.MAIN), title, Seq(Revision.one(text1)))
      val page2 =
        Page(None, Some(Namespace.FILE), title, Seq(Revision.one(text2)))

      val pageId1 = pageDao.insert(page1)
      val pageId2 = pageDao.insert(page2)

      val dbPage1 = pageDao.withText(pageId1)
      val dbPage2 = pageDao.withText(pageId2)

      dbPage1.title === "title"
      dbPage2.title === "title"

      dbPage1.text === Some(text1)
      dbPage2.text === Some(text2)
    }

    "insert with two revisions" in {
      createSchema()

      val texts = Seq("rev2", "rev1")
      val revisions = texts.map(Revision.one)

      val page = Page(None, Some(0), "title", revisions)

      val pageId = pageDao.insert(page)

      val dbPage = pageDao.withText(pageId)

      dbPage.text === Some(texts.head)
      dbPage.revisions.size === 1

      val withRevisions = pageDao.withRevisions(pageId)
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
      val pages = names.zip(texts).map { case (n, t) =>
        Page(None, Some(0), n, Seq(Revision.one(t)))
      }

      val pageIds = pages.flatMap(p => pageDao.insert(p).toOption)
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
      val pages = names.zip(texts).map { case (n, t) =>
        Page(None, Some(0), n, Seq(Revision.one(t)))
      }

      val pageIds = pages.flatMap(p => pageDao.insert(p).toOption)
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
      val pages = names.zip(texts).map { case (n, t) =>
        Page(None, Some(0), n, Seq(Revision.one(t)))
      }

      val pageIds = pages.flatMap(p => pageDao.insert(p).toOption)
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
      val pages = names.zip(texts).map { case (n, t) =>
        Page(None, Some(0), n, t.map(Revision.one))
      }

      val pageIds = pages.flatMap(p => pageDao.insert(p).toOption.toSeq)
      pageIds.size === 3

      val withRevisions = pageIds.map(id => pageDao.withRevisions(id))

      withRevisions.map(_.revisions.size) === Seq(2, 2, 2)

      val revIds = withRevisions.flatMap(_.revisions.last.id)

      val dbPages = pageDao.findByRevIds(
        Set(pageIds.head, pageIds.last),
        Set(revIds.head, revIds.last)
      )
      dbPages.size === 2
      dbPages.map(_.title) === Seq("page1", "page3")
      dbPages.flatMap(_.text.toSeq) === Seq("page1Text1", "page3Text1")
    }

    "find pages by revIdsOpt" in {
      createSchema()

      val names = Seq("page1", "page2", "page3")
      val texts = Seq(
        Seq("page1Text2", "page1Text1"),
        Seq("page2Text2", "page2Text1"),
        Seq("page3Text2", "page3Text1")
      )
      //      val revisions = texts.map(Revision.one)
      val pages = names.zip(texts).map { case (n, t) =>
        Page(None, Some(0), n, t.map(Revision.one))
      }

      val pageIds = pages.flatMap(p => pageDao.insert(p).toOption.toSeq)
      pageIds.size === 3

      val withRevisions = pageIds.map(id => pageDao.withRevisions(id))

      withRevisions.map(_.revisions.size) === Seq(2, 2, 2)

      val revIds = withRevisions.flatMap(_.revisions.last.id)

      val dbPages = pageDao.findByPageAndRevIdsOpt(
        Set(pageIds.head, pageIds.last),
        Set(revIds.head, revIds.last)
      )
      dbPages.size === 2
      dbPages.map(_.title) === Seq("page1", "page3")
      dbPages.flatMap(_.text.toSeq) === Seq("page1Text1", "page3Text1")

      val dbPagesOpt = pageDao.findByPageAndRevIdsOpt(
        Set(pageIds.head, pageIds.last),
        Set(revIds.head, 123L)
      )
      dbPagesOpt.size === 2
      dbPagesOpt.map(_.title) === Seq("page1", "page3")
      dbPagesOpt.flatMap(_.text.toSeq) === Seq("page1Text1")
    }
  }
}
