package org.scalawiki.sql

import org.scalawiki.MwBot
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.list.{EmbeddedIn, ListArgs}
import org.scalawiki.dto.cmd.query.prop.rvprop.{Content, Ids, RvProp}
import org.scalawiki.dto.cmd.query.prop.{Info, Prop, Revisions, RvPropArgs}
import org.scalawiki.dto.cmd.query.{Generator, PageIdsParam, Query}
import org.scalawiki.dto.{Page, Revision, User}
import org.scalawiki.query.{DslQuery, DummyActionArg}
import org.scalawiki.util.{HttpStub, MockBotSpec}
import org.specs2.mutable.{BeforeAfter, Specification}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import spray.util.pimpFuture

class DslQueryDbCacheModularSpec extends Specification with MockBotSpec with BeforeAfter {

  sequential

  def database = mwDb

  var bot: MwBot = _

  var dc: DatabaseConfig[JdbcProfile] = _

  var mwDb: MwDatabase = _

  def pageDao = mwDb.pageDao

  def revisionDao = mwDb.revisionDao

  def textDao = mwDb.textDao

  val dummyAction = Action(DummyActionArg)

  override def getBot(commands: HttpStub*) = {
    val apiBot = super.getBot(commands:_*)

    mwDb.dropTables()
    mwDb.createTables()

    new DbCachedBot(apiBot, mwDb)
  }

  override def before = {
    dc = DatabaseConfig.forConfig[JdbcProfile]("h2mem")
    mwDb = new MwDatabase(dc)
  }

  override def after = {
    //dc.db.close()
  }

  val pageText1 = "some vandalism"
  val pageText2 = "more vandalism"
  val emptyUser = Some(User(0, ""))
  val someUser1 = Some(User(7L, "Formator"))
  val someUser2 = Some(User(9L, "OtherUser"))

  def revisionContent(content: Option[String]) = content.fold("") {
    text => s""", "*": "$text" """
  }

  def page(pageId: Long, title: String, revId: Long, content: Option[String] = None, ns: Int = 0, user: String = "Formator", userId: Long = 7) =
    s""" "$pageId":
    { "pageid": $pageId, "ns": $ns, "title": "$title",
      "revisions": [{"revid": $revId, "user": "$user", "userid": $userId ${revisionContent(content)} }]
    }"""

  def page1(content: Option[String] = None, revId: Long = 11, user: String = "OtherUser", userId: Long = 9) =
    page(569559, "Talk:Welfare reform", revId, content, 1, user, userId)

  def page2(content: Option[String] = None, revId: Long = 12, user: String = "Formator", userId: Long = 7) =
    page(4571809, "User:Formator", revId, content, 2, user, userId)

  def pagesJson(pages: Seq[String]) =
    s"""{ "query": {"pages": { ${pages.mkString(", ")} }}}"""

  def responseWithRevIds(pages: Seq[String] = Seq(page2(), page1())) =
    pagesJson(pages)

  def responseWithContent(pages: Seq[String] = Seq(page2(Some(pageText2)), page1(Some(pageText1)))) =
    pagesJson(pages)



  "toDb" should {

    "insert new pages" in {
      bot = getBot()

      val cache = new DslQueryDbCache(new DslQuery(dummyAction, bot), database)

      val (titles, texts, pageIds, revIds) = (
        (1 to 10) map ("title" + _),
        (1 to 10) map ("text" + _),
        51L to 60L,
        21L to 30L
        )

      val pages = titles.zip(texts).zip(pageIds).zip(revIds) map {
        case (((title, text), pageId), revId) =>
          val revision: Revision = new Revision(revId = Some(revId), pageId = Some(pageId), content = Some(text))
          Page(Some(pageId), Some(0), title, Seq(revision))
      }

      cache.toDb(pages, Set.empty)

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

    "insert new revisions" in {
      bot = getBot()

      val cache = new DslQueryDbCache(new DslQuery(dummyAction, bot), database)

      val (titles, texts, pageIds, revIds) = (
        (1 to 10) map ("title" + _),
        (1 to 10) map ("text" + _),
        51L to 60L,
        21L to 30L
        )

      val pages = titles.zip(texts).zip(pageIds).zip(revIds) map {
        case (((title, text), pageId), revId) =>
          val revision: Revision = new Revision(revId = Some(revId), pageId = Some(pageId), content = Some(text))
          Page(Some(pageId), Some(0), title, Seq(revision))
      }

      pageDao.insertAll(pages)

      val newPages = pages.map {
        p =>
          val rev = p.revisions.head
          val newRev = rev.copy(revId = rev.id.map(_ + 100), content = rev.content.map("new_" + _))
          p.copy(revisions = Seq(newRev))
      }

      cache.toDb(newPages, pages.flatMap(_.id).toSet)

      val dbPages = pageDao.list
      dbPages.flatMap(_.revisions.headOption.flatMap(_.revId)) aka "revIds in pages" must_== revIds.map(_ + 100)

      val fromDb = pageDao.listWithText

      fromDb.size aka "pages with text size" must_== 10

      (0 to 9) map { i =>
        val page = fromDb(i)
        page.id === Some(pageIds(i))
        page.title === titles(i)
        page.text === Some("new_" + texts(i))
        page.revisions.size === 1
        page.revisions.head.revId === Some(revIds(i) + 100)
      }
    }

  }

  "notInDb" should {
    "return empty seq" in {
      bot = getBot()

      val cache = new DslQueryDbCache(new DslQuery(dummyAction, bot), database)

      val (titles, texts, pageIds, revIds) = (
        (1 to 10) map ("title" + _),
        (1 to 10) map ("text" + _),
        51L to 60L,
        21L to 30L
        )

      val dbPages = titles.zip(texts).zip(pageIds).zip(revIds) map {
        case (((title, text), pageId), revId) =>
          val revision: Revision = new Revision(revId = Some(revId), pageId = Some(pageId), content = Some(text))
          Page(Some(pageId), Some(0), title, Seq(revision))
      }

      val query = Query()
      val notInDb = cache.notInDb(query, pageIds.toSet, dbPages).await

      notInDb.size === 0
    }

    "return updated revs" in {
      val (titles, texts, pageIds, revIds) = (
        (1 to 10) map ("title" + _),
        (1 to 10) map ("text" + _),
        51L to 60L,
        21L to 30L
        )

      val pageJsons = titles.zip(texts).zip(pageIds).zip(revIds) map {
        case (((title, text), pageId), revId) =>
          page(pageId, title, revId, Some(text))
      }

      val commands = Seq(
        // fetch for page2 content for cache
        HttpStub(Map("action" -> "query",
          "pageids" -> pageIds.drop(5).mkString("|"),
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid", "continue" -> ""),
          pagesJson(pageJsons.drop(5)))
      )

      bot = getBot(commands: _*)

      val query = Query(
        Prop(
          Info(),
          Revisions(RvProp(RvPropArgs.byNames(Seq("ids", "content", "user", "userid")): _*))
        ),
        Generator(ListArgs.toDsl("categorymembers", Some("Category:SomeCategory"), None, Set.empty, Some("max")).get)
      )

      val cache = new DslQueryDbCache(new DslQuery(Action(query), bot), database)

      val dbPages = titles.zip(texts).zip(pageIds).zip(revIds) map {
        case (((title, text), pageId), revId) =>
          val revision = new Revision(
            revId = Some(revId),
            pageId = Some(pageId),
            content = Some(text),
            user = someUser1)
          Page(Some(pageId), Some(0), title, Seq(revision))
      }

      val slice = dbPages.take(5) ++ dbPages.drop(5).map(_.copy(revisions = Seq.empty))
      val notInDb = cache.notInDb(query, pageIds.toSet, slice).await

      notInDb === dbPages.drop(5)
    }

    "return by generator" in {
      val (titles, texts, pageIds, revIds) = (
        (1 to 100) map ("title" + _),
        (1 to 100) map ("text" + _),
        100L to 199L,
        200L to 299L
        )

      val pageJsons = titles.zip(texts).zip(pageIds).zip(revIds) map {
        case (((title, text), pageId), revId) =>
          page(pageId, title, revId, Some(text))
      }

      val commands = Seq(
        HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid",
          "continue" -> ""), pagesJson(pageJsons))
      )

      bot = getBot(commands: _*)

      val query = Query(
        Prop(
          Info(),
          Revisions(RvProp(RvPropArgs.byNames(Seq("ids", "content", "user", "userid")): _*))
        ),
        Generator(ListArgs.toDsl("categorymembers", Some("Category:SomeCategory"), None, Set.empty, Some("max")).get)
      )

      val cache = new DslQueryDbCache(new DslQuery(Action(query), bot), database)

      val dbPages = titles.zip(texts).zip(pageIds).zip(revIds) map {
        case (((title, text), pageId), revId) =>
          val revision = new Revision(
            revId = Some(revId),
            pageId = Some(pageId),
            content = Some(text),
            user = someUser1)
          Page(Some(pageId), Some(0), title, Seq(revision))
      }

      val notInDb = cache.notInDb(query, pageIds.toSet, Seq.empty).await

      notInDb === dbPages
    }


    "return in batches" in {
      val (titles, texts, pageIds, revIds) = (
        (1 to 100) map ("title" + _),
        (1 to 100) map ("text" + _),
        100L to 199L,
        200L to 299L
        )

      val pageJsons = titles.zip(texts).zip(pageIds).zip(revIds) map {
        case (((title, text), pageId), revId) =>
          page(pageId, title, revId, Some(text))
      }

      val commands = Seq(
        // fetch for page2 content for cache
        HttpStub(Map("action" -> "query",
          "pageids" -> pageIds.slice(5, 55).mkString("|"),
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid", "continue" -> ""),
          pagesJson(pageJsons.slice(5, 55))),

        HttpStub(Map("action" -> "query",
          "pageids" -> pageIds.slice(55, 95).mkString("|"),
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid", "continue" -> ""),
          pagesJson(pageJsons.slice(55, 95)))
      )

      bot = getBot(commands: _*)

      val query = Query(
        Prop(
          Info(),
          Revisions(RvProp(RvPropArgs.byNames(Seq("ids", "content", "user", "userid")): _*))
        ),
        Generator(ListArgs.toDsl("categorymembers", Some("Category:SomeCategory"), None, Set.empty, Some("max")).get)
      )

      val cache = new DslQueryDbCache(new DslQuery(Action(query), bot), database)

      val dbPages = titles.zip(texts).zip(pageIds).zip(revIds) map {
        case (((title, text), pageId), revId) =>
          val revision = new Revision(
            revId = Some(revId),
            pageId = Some(pageId),
            content = Some(text),
            user = someUser1)
          Page(Some(pageId), Some(0), title, Seq(revision))
      }

      val slice = dbPages.take(5) ++
        dbPages.slice(5, 95).map(_.copy(revisions = Seq.empty)) ++
        dbPages.drop(95)
      val notInDb = cache.notInDb(query, pageIds.toSet, slice).await

      notInDb === dbPages.slice(5, 95)
    }
  }

  "cache" should {
    "return removed revision content parameter" in {

      val namespaces = Set(0)
      val limit = Some("max")
      val title = Some("Template:WLM-row")

      val query = Query(
        Prop(
          Info(),
          Revisions(RvProp(Ids, Content))
        ),
        Generator(
          new EmbeddedIn(title, None, namespaces, limit)
        )
      )

      val notInDbIds = Seq(1L, 2L, 3L)
      val notInDbQuery = new DslQueryDbCache(new DslQuery(Action(query), super.getBot()), database).notInDBQuery(query, notInDbIds)

      notInDbQuery === Seq(Query(
        Prop(
          Info(),
          Revisions(RvProp(Ids, Content))
        ),
        PageIdsParam(notInDbIds)
      ))
    }
  }

}


