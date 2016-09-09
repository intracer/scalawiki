package org.scalawiki.sql

import org.scalawiki.dto.{Page, Revision, User}
import org.scalawiki.dto.Image
import org.specs2.mutable.{BeforeAfter, Specification}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

class PageDaoBulkSpec extends Specification with BeforeAfter {

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
    //mwDb.db.close()
  }

  "page" should {
    "not insert without revision" in {
      createSchema()

      val pages = (1 to 10) map (i => new Page(None, 0, "title" + i))
      pageDao.insertAll(pages) must throwA[IllegalArgumentException]
      pageDao.count === 0
    }

    "insert with revision" in {
      createSchema()

      val (titles, texts) = (
        (1 to 10) map ("title" + _),
        (1 to 10) map ("text" + _)
        )

      val pages = titles.zip(texts) map {
        case (title, text) =>
          Page(None, 0, title, Seq(Revision.one(text)))
      }

      pageDao.insertAll(pages)

      textDao.count aka "text size" must_== 10
      revisionDao.count aka "revisions size" must_== 10
      pageDao.count aka "pages size" must_== 10

      val dbTexts = textDao.list
      dbTexts.map(_.text) aka "texts" must_== texts

      val revs = revisionDao.list
      revs.map(_.textId) aka "textIds in revisions" must_== dbTexts.map(_.id)

      val dbPages = pageDao.list
      dbPages.map(_.revisions.headOption.flatMap(_.revId)) aka "revIds in pages" must_== revs.map(_.id)

      val fromDb = pageDao.listWithText

      fromDb.size aka "pages with text size" must_== 10

      (0 to 9) map { i =>
        val page = fromDb(i)
        page.title === titles(i)
        page.text === Some(texts(i))
        page.revisions.size === 1
      }
    }

    "insert with given id page id" in {
      createSchema()

      val (titles, texts, pageIds) = (
        (1 to 10) map ("title" + _),
        (1 to 10) map ("text" + _),
        51L to 60L
        )

      val pages = titles.zip(texts).zip(pageIds) map {
        case ((title, text), pageId) =>
          Page(Some(pageId), 0, title, Seq(Revision.one(text)))
      }

      pageDao.insertAll(pages)

      textDao.count aka "text size" must_== 10
      revisionDao.count aka "revisions size" must_== 10
      pageDao.count aka "pages size" must_== 10

      val dbTexts = textDao.list
      dbTexts.map(_.text) aka "texts" must_== texts

      val revs = revisionDao.list
      revs.map(_.textId) aka "textIds in revisions" must_== dbTexts.map(_.id)

      val dbPages = pageDao.list
      dbPages.map(_.revisions.headOption.flatMap(_.revId)) aka "revIds in pages" must_== revs.map(_.id)

      val fromDb = pageDao.listWithText

      fromDb.size aka "pages with text size" must_== 10

      (0 to 9) map { i =>
        val page = fromDb(i)
        page.id === Some(pageIds(i))
        page.title === titles(i)
        page.text === Some(texts(i))
        page.revisions.size === 1
      }
    }

    "insert with given id page id and revision id" in {
      createSchema()

      val (titles, texts, pageIds, revIds) = (
        (1 to 10) map ("title" + _),
        (1 to 10) map ("text" + _),
        51L to 60L,
        21L to 30L
        )

      val pages = titles.zip(texts).zip(pageIds).zip(revIds) map {
        case (((title, text), pageId), revId) =>
          val revision: Revision = new Revision(revId = Some(revId), pageId = Some(pageId), content = Some(text))
          Page(Some(pageId), 0, title, Seq(revision))
      }

      pageDao.insertAll(pages)

      textDao.count aka "text size" must_== 10
      revisionDao.count aka "revisions size" must_== 10
      pageDao.count aka "pages size" must_== 10

      val dbTexts = textDao.list
      dbTexts.map(_.text) aka "texts" must_== texts

      val revs = revisionDao.list
      revs.map(_.textId) aka "textIds in revisions" must_== dbTexts.map(_.id)

      val dbPages = pageDao.list
      dbPages.map(_.revisions.headOption.flatMap(_.revId)) aka "revIds in pages" must_== revs.map(_.id)

      val fromDb = pageDao.listWithText

      fromDb.size aka "pages with text size" must_== 10

      (0 to 9) map { i =>
        val page = fromDb(i)
        page.id === Some(pageIds(i))
        page.title === titles(i)
        page.text === Some(texts(i))
        page.revisions.size === 1
        page.revisions.head.revId === Some(revIds(i))
      }
    }

    "insert with users" in {
      createSchema()

      val (titles, texts, pageIds, revIds, userIds, userNames) = (
        (1 to 10) map ("title" + _),
        (1 to 10) map ("text" + _),
        11L to 20L,
        21L to 30L,
        31L to 40L,
        (1 to 10) map ("user" + _)
        )

      val pages = (0 to 9) map {
        i =>
          val revision: Revision = new Revision(revId = Some(revIds(i)), pageId = Some(pageIds(i)), content = Some(texts(i)),
            user = Some(User(userIds(i), userNames(i))))
          Page(Some(pageIds(i)), 0, titles(i), Seq(revision))
      }

      pageDao.insertAll(pages)

      textDao.count aka "text size" must_== 10
      revisionDao.count aka "revisions size" must_== 10
      pageDao.count aka "pages size" must_== 10

      val dbTexts = textDao.list
      dbTexts.map(_.text) aka "texts" must_== texts

      val revs = revisionDao.list
      revs.map(_.textId) aka "textIds in revisions" must_== dbTexts.map(_.id)

      val dbPages = pageDao.list
      dbPages.map(_.revisions.headOption.flatMap(_.revId)) aka "revIds in pages" must_== revs.map(_.id)

      val fromDb = pageDao.listWithText

      fromDb.size aka "pages with text size" must_== 10

      (0 to 9) map { i =>
        val page = fromDb(i)
        page.id === Some(pageIds(i))
        page.title === titles(i)
        page.text === Some(texts(i))
        page.revisions.size === 1
        page.revisions.head.revId === Some(revIds(i))
        page.revisions.head.user === Some(User(userIds(i), userNames(i)))
      }

      userDao.count aka "users count" must_== 10
    }

    "insert with image" in {
      createSchema()

      val user = User(5, "username")
      val title = "Image.jpg"
      val image = new Image(
        title,
        Some("http://Image.jpg"), None,
        Some(1000 * 1000), Some(800), Some(600),
        user.name, Some(user)
      )
      val revision: Revision = new Revision(user = Some(user), content = Some("revision text"))

      val page = Page(None, 0, title, Seq(revision), Seq(image))

      val pageId = pageDao.insertAll(Seq(page)).head

      eventually {
        val images = imageDao.list
        images.size === 1
        images.head === image.copy(pageId = pageId)

        val dbImage = imageDao.get(title)
        dbImage === Some(image.copy(pageId = pageId))
      }
    }
  }
}
