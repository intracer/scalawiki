package org.scalawiki.query

import java.util.concurrent.TimeUnit

import org.scalawiki.MwBot
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.list.EmbeddedIn
import org.scalawiki.dto.cmd.query.prop.rvprop.{Content, Ids, RvProp}
import org.scalawiki.dto.cmd.query.prop.{Info, Prop, Revisions}
import org.scalawiki.dto.cmd.query.{Generator, PageIdsParam, Query}
import org.scalawiki.dto.{Page, Revision, User}
import org.scalawiki.sql.MwDatabase
import org.scalawiki.util.{Command, MockBotSpec}
import org.specs2.mutable.{BeforeAfter, Specification}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import spray.util.pimpFuture

// TODO check 50 pages limit
class DslQueryDbCacheSpec extends Specification with MockBotSpec with BeforeAfter {

  sequential


  override def database = Some(mwDb)

  var bot: MwBot = _

  var dc: DatabaseConfig[JdbcProfile] = _

  var mwDb: MwDatabase = _

  def pageDao = mwDb.pageDao

  def revisionDao = mwDb.revisionDao

  override def before = {
    dc = DatabaseConfig.forConfig[JdbcProfile]("h2mem")
    mwDb = new MwDatabase(dc.db)
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

  def page2(content: Option[String] = None, revId: Long = 12, user: String = "Formator", userId: Long = 7) = s""" "4571809":
    { "pageid": 4571809, "ns": 2, "title": "User:Formator",
      "revisions": [{"revid": $revId, "user": "$user", "userid": $userId ${revisionContent(content)} }]
    }"""

  def page1(content: Option[String] = None, revId: Long = 11, user: String = "OtherUser", userId: Long = 9) = s""" "569559":
    { "pageid": 569559, "ns": 1, "title": "Talk:Welfare reform",
      "revisions": [{"revid": $revId, "user": "$user", "userid": $userId ${revisionContent(content)} }]
    }"""

  def pagesJson(pages: Seq[String]) =
    s"""{ "query": {"pages": { ${pages.mkString(", ")} }}}"""

  def responseWithRevIds(pages: Seq[String] = Seq(page2(), page1())) =
    pagesJson(pages)

  def responseWithContent(pages: Seq[String] = Seq(page2(Some(pageText2)), page1(Some(pageText1)))) =
    pagesJson(pages)

  "get revisions text with generator and caching" should {
    "first run should use generator" in {
      val expectedCommands = Seq(new Command(Map(
        "action" -> "query",
        "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
        "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",  // TODO remove user?
        "continue" -> ""), responseWithRevIds()),
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid",
          "continue" -> ""), responseWithContent())
      )

      bot = getBot(expectedCommands: _*)

      val future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "userid"))

      val result = future.await(timeout = 15.minutes)

      result must have size 2

      result(0) === Page(Some(4571809L), 2, "User:Formator",
        Seq(Revision(Some(12L), Some(4571809L), None, someUser1, None, None, Some(pageText2)))
      )
      result(1) === Page(Some(569559L), 1, "Talk:Welfare reform",
        Seq(Revision(Some(11L), Some(569559L), None, someUser2, None, None, Some(pageText1)))
      )
    }

    "from cache in" in {

      val expectedCommands = Seq(
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), responseWithRevIds()),

        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid",
          "continue" -> ""), responseWithContent()),

        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), responseWithRevIds())
      )

      bot = getBot(expectedCommands: _*)

      val future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "userid"))

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))

      result must have size 2
      result(0) === Page(Some(4571809L), 2, "User:Formator",
        Seq(Revision(Some(12L), Some(4571809L), None, someUser1, None, None, Some(pageText2)))
      )
      result(1) === Page(Some(569559L), 1, "Talk:Welfare reform",
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

      val resultDb = futureDb.await
      resultDb must have size 2
      resultDb(0) === Page(Some(569559L), 1, "Talk:Welfare reform",
        Seq(Revision(Some(11L), Some(569559L), Some(0), someUser2, None, None, Some(pageText1),
          textId = resultDb(0).revisions.head.textId))
      )
      resultDb(1) === Page(Some(4571809L), 2, "User:Formator",
        Seq(Revision(Some(12L), Some(4571809L), Some(0), someUser1, None, None, Some(pageText2),
          textId = resultDb(1).revisions.head.textId))
      )
    }

    "add page to cache" in {
      val expectedCommands = Seq(
      // query for page 1 ids in category
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), pagesJson(Seq(page1()))
        ),

        // query for page 1 content
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(Some(pageText1))))
        ),

        // query for page 1 and page2  ids in category
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(), page2()))
        ),

        // fetch for page2 content for cache
        new Command(Map("action" -> "query",
          "pageids" -> "4571809",
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid", "continue" -> ""), pagesJson(Seq(page2(Some(pageText2))))
        ),

        // query for page 1 and page2  ids in category. content should be in cache by now
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(), page2()))
        )
      )

      bot = getBot(expectedCommands: _*)

      val future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "userid"))

      val result = future.await

      result must have size 1
      result(0) === Page(Some(569559L), 1, "Talk:Welfare reform",
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

      val plus1 = plus1Future.await
      plus1 must have size 2
      plus1(0) === Page(Some(569559L), 1, "Talk:Welfare reform",
        Seq(Revision(Some(11L), Some(569559L), Some(0), someUser2, None, None, Some(pageText1),
          textId = plus1(0).revisions.head.textId))
      )
      plus1(1) === Page(Some(4571809L), 2, "User:Formator",
        Seq(Revision(Some(12L), Some(4571809L), None, someUser1, None, None, Some(pageText2)))
      )

      pageDao.list.size must be_==(2).eventually
      val byRevsFinal = pageDao.findByRevIds(Seq(4571809L, 569559L), Seq(12, 11))
      byRevsFinal.size === 2
      byRevsFinal.map(_.title) === Seq("Talk:Welfare reform", "User:Formator")
      byRevsFinal.map(_.revisions.head.content.get) === Seq("some vandalism", "more vandalism")

      val futureFinal = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "userid"))

      val resultFinal = futureFinal.await
      resultFinal must have size 2
      resultFinal(0) === Page(Some(569559L), 1, "Talk:Welfare reform",
        Seq(Revision(Some(11L), Some(569559L), Some(0), someUser2, None, None, Some(pageText1),
          textId = resultFinal(0).revisions.head.textId))
      )
      resultFinal(1) === Page(Some(4571809L), 2, "User:Formator",
        Seq(Revision(Some(12L), Some(4571809L), Some(0), someUser1, None, None, Some(pageText2),
          textId = resultFinal(1).revisions.head.textId))
      )
    }

    "add one page revision to cache" in { // TODO more pages?

      val expectedCommands = Seq(
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(revId = 11)))
        ),

        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(Some(pageText1), revId = 11)))
        ),

        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(revId = 12)))
        ),

        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(Some(pageText2), revId = 12)))
        ),

        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|user|userid",
          "continue" -> ""), pagesJson(Seq(page1(revId = 12)))
        )
      )

      bot = getBot(expectedCommands: _*)

      val future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "userid"))

      val result = future.await

      result must have size 1
      result(0) === Page(Some(569559L), 1, "Talk:Welfare reform",
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
      plus1(0) === Page(Some(569559L), 1, "Talk:Welfare reform",
        Seq(Revision(Some(12L), Some(569559L), None, someUser2, None, None, Some(pageText2)))
      )

      revisionDao.list.size must be_==(2).eventually
      val byRevsFinal = pageDao.findByRevIds(Seq(569559L), Seq(12))
      byRevsFinal.size === 1
      byRevsFinal.map(_.title) === Seq("Talk:Welfare reform")
      byRevsFinal.map(_.revisions.head.content.get) === Seq("more vandalism")

      val futureFinal = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "userid"))

      val resultFinal = futureFinal.await
      resultFinal must have size 1
      resultFinal(0) === Page(Some(569559L), 1, "Talk:Welfare reform",
        Seq(Revision(Some(12L), Some(569559L), Some(0), someUser2, None, None, Some(pageText2),
          textId = resultFinal(0).revisions.head.textId))
      )
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
      val notInDbQuery = new DslQueryDbCache(new DslQuery(Action(query), null)).notInDBQuery(query, notInDbIds)

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
