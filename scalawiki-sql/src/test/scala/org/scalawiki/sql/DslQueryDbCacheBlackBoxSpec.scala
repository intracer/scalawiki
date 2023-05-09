package org.scalawiki.sql

import java.util.concurrent.TimeUnit

import org.scalawiki.MwBot
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.{Page, Revision, User}
import org.scalawiki.query.DummyActionArg
import org.scalawiki.util.{HttpStub, MockBotSpec}
import org.specs2.mutable.{BeforeAfter, Specification}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import spray.util.pimpFuture

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}

class DslQueryDbCacheBlackBoxSpec extends Specification with MockBotSpec with BeforeAfter {

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

    new DbCachedBot(apiBot, database)
  }

  override def before = {
    dc = DatabaseConfig.forConfig[JdbcProfile]("h2mem")
    mwDb = new MwDatabase(dc)
  }

  override def after = {
 //   dc.db.close()
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

  "get revisions text with generator and caching" should {
    "first run should use generator" in {
      val expectedCommands = Seq(HttpStub(Map(
        "action" -> "query",
        "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
        "prop" -> "info|revisions", "rvprop" -> "ids|user|userid", // TODO remove user?
        "continue" -> ""), responseWithRevIds()),

        HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid",
          "continue" -> ""), responseWithContent())
      )

      bot = getBot(expectedCommands: _*)

      val future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "userid"))

      val result = future.await(timeout = 1.minutes).toSeq

      result must have size 2

      result.head === Page(Some(4571809L), Some(2), "User:Formator",
        Seq(Revision(Some(12L), Some(4571809L), None, someUser1, None, None, Some(pageText2)))
      )
      result(1) === Page(Some(569559L), Some(1), "Talk:Welfare reform",
        Seq(Revision(Some(11L), Some(569559L), None, someUser2, None, None, Some(pageText1)))
      )
    }

    "from cache in" in {

      val expectedCommands = Seq(
        HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), responseWithRevIds()),

        HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid",
          "continue" -> ""), responseWithContent()),

        HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), responseWithRevIds())
      )

      bot = getBot(expectedCommands: _*)

      val future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "userid"))

      val result = future.await.toSeq

      result must have size 2
      result(0) === Page(Some(4571809L), Some(2), "User:Formator",
        Seq(Revision(Some(12L), Some(4571809L), None, someUser1, None, None, Some(pageText2)))
      )
      result(1) === Page(Some(569559L), Some(1), "Talk:Welfare reform",
        Seq(Revision(Some(11L), Some(569559L), None, someUser2, None, None, Some(pageText1)))
      )

      pageDao.list.size must be_==(2).eventually
      val byRevs = pageDao.findByRevIds(Seq(4571809L, 569559L), Seq(12, 11))
      byRevs.size === 2
      byRevs.map(_.title) === Seq("Talk:Welfare reform", "User:Formator")
      byRevs.map(_.revisions.head.id.get) === Seq(11L, 12L)
      byRevs.map(_.revisions.head.content.get) === Seq("some vandalism", "more vandalism")

      val futureDb = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "userid"))

      val resultDb = futureDb.await.toSeq
      resultDb must have size 2
      resultDb.head === Page(Some(569559L), Some(1), "Talk:Welfare reform",
        Seq(Revision(Some(11L), Some(569559L), Some(0), someUser2, None, None, Some(pageText1),
          textId = resultDb.head.revisions.head.textId))
      )
      resultDb(1) === Page(Some(4571809L), Some(2), "User:Formator",
        Seq(Revision(Some(12L), Some(4571809L), Some(0), someUser1, None, None, Some(pageText2),
          textId = resultDb(1).revisions.head.textId))
      )
    }

    "add page to cache" in {
      val expectedCommands = Seq(
        // query for page 1 ids in category
        HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), pagesJson(Seq(page1()))
        ),

        // query for page 1 content
        HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(Some(pageText1))))
        ),

        // query for page 1 and page2  ids in category
        HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(), page2()))
        ),

        // fetch for page2 content for cache
        HttpStub(Map("action" -> "query",
          "pageids" -> "4571809",
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid", "continue" -> ""),
          pagesJson(Seq(page2(Some(pageText2))))
        ),

        // query for page 1 and page2  ids in category. content should be in cache by now
        HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(), page2()))
        )
      )

      bot = getBot(expectedCommands: _*)

      val future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "userid"))

      val result = future.await.toSeq

      result must have size 1
      result(0) === Page(Some(569559L), Some(1), "Talk:Welfare reform",
        Seq(Revision(Some(11L), Some(569559L), None, someUser2, None, None, Some(pageText1)))
      )

      pageDao.list.size must be_==(1).eventually
      val byRevs = pageDao.findByRevIds(Seq(569559L), Seq(11))
      byRevs.size === 1
      byRevs.map(_.title) === Seq("Talk:Welfare reform")
      byRevs.map(_.revisions.head.id.get) === Seq(11L)
      byRevs.map(_.revisions.head.content.get) === Seq("some vandalism")

      val plus1Future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "userid"))

      val plus1 = plus1Future.await.toSeq
      plus1 must have size 2
      plus1(0) === Page(Some(569559L), Some(1), "Talk:Welfare reform",
        Seq(Revision(Some(11L), Some(569559L), Some(0), someUser2, None, None, Some(pageText1),
          textId = plus1(0).revisions.head.textId))
      )
      plus1(1) === Page(Some(4571809L), Some(2), "User:Formator",
        Seq(Revision(Some(12L), Some(4571809L), None, someUser1, None, None, Some(pageText2)))
      )

      pageDao.list.size must be_==(2).eventually
      val byRevsFinal = pageDao.findByRevIds(Seq(4571809L, 569559L), Seq(12, 11))
      byRevsFinal.size === 2
      byRevsFinal.map(_.title) === Seq("Talk:Welfare reform", "User:Formator")
      byRevsFinal.map(_.revisions.head.content.get) === Seq("some vandalism", "more vandalism")

      val futureFinal = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "userid"))

      val resultFinal = futureFinal.await.toSeq
      resultFinal must have size 2
      resultFinal(0) === Page(Some(569559L), Some(1), "Talk:Welfare reform",
        Seq(Revision(Some(11L), Some(569559L), Some(0), someUser2, None, None, Some(pageText1),
          textId = resultFinal(0).revisions.head.textId))
      )
      resultFinal(1) === Page(Some(4571809L), Some(2), "User:Formator",
        Seq(Revision(Some(12L), Some(4571809L), Some(0), someUser1, None, None, Some(pageText2),
          textId = resultFinal(1).revisions.head.textId))
      )
    }

    "add one page revision to cache" in {
      // TODO more pages?

      val expectedCommands = Seq(
        HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(revId = 11)))
        ),

        HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(Some(pageText1), revId = 11)))
        ),

        HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(revId = 12)))
        ),

        HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(Some(pageText2), revId = 12)))
        ),

        HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(revId = 12)))
        )
      )

      bot = getBot(expectedCommands: _*)

      val future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "userid"))

      val result = future.await.toSeq

      result must have size 1
      result(0) === Page(Some(569559L), Some(1), "Talk:Welfare reform",
        Seq(Revision(Some(11L), Some(569559L), None, someUser2, None, None, Some(pageText1)))
      )

      pageDao.list.size must be_==(1).eventually
      val byRevs = pageDao.findByRevIds(Seq(569559L), Seq(11))
      byRevs.size === 1
      byRevs.map(_.title) === Seq("Talk:Welfare reform")
      byRevs.map(_.revisions.head.content.get) === Seq("some vandalism")

      val plus1Future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "userid"))

      val plus1 = plus1Future.await
      plus1 must have size 1
      plus1(0) === Page(Some(569559L), Some(1), "Talk:Welfare reform",
        Seq(Revision(Some(12L), Some(569559L), None, someUser2, None, None, Some(pageText2)))
      )

      revisionDao.list.size must be_==(2).eventually
      val byRevsFinal = pageDao.findByRevIds(Seq(569559L), Seq(12))
      byRevsFinal.size === 1
      byRevsFinal.map(_.title) === Seq("Talk:Welfare reform")
      byRevsFinal.map(_.revisions.head.content.get) === Seq("more vandalism")

      val futureFinal = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "userid"))

      val resultFinal = futureFinal.await.toSeq
      resultFinal must have size 1
      resultFinal(0) === Page(Some(569559L), Some(1), "Talk:Welfare reform",
        Seq(Revision(Some(12L), Some(569559L), Some(0), someUser2, None, None, Some(pageText2),
          textId = resultFinal(0).revisions.head.textId))
      )
    }
  }
}
