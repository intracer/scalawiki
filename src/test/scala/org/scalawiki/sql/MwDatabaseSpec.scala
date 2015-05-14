package org.scalawiki.sql


import org.specs2.mutable.{BeforeAfter, Specification}

import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable
import scala.slick.lifted.TableQuery

class MwDatabaseSpec extends Specification with BeforeAfter {

  sequential

  implicit var session: Session = _

  val mwDb = new MwDatabase()

  def createSchema() = mwDb.createTables()

  override def before = {
    // session = Database.forURL("jdbc:h2:~/test", driver = "org.h2.Driver").createSession()
    session = Database.forURL("jdbc:h2:mem:test", driver = "org.h2.Driver").createSession()
  }

  override def after = session.close()

  "ddls" should {
    "create schema" in {
      createSchema()

      getTableNames === Set("category",
        "image",
        "page",
        "revision",
        "text",
        "user")
    }

    "create table with custom prefix" in {
      val ukWikiPages = TableQuery[Pages]((tag: Tag) => new Pages(tag, "ukwiki_page", Some("ukwiki")))

      mwDb.dropTables()
      mwDb.createIfNotExists(ukWikiPages)

      val names = getTableNames
      ukWikiPages.ddl.drop

      names === Set("ukwiki_page")
    }
   }

  def getTableNames: Set[String] = {
    val tables = MTable.getTables.list
    tables.map(_.name.name.toLowerCase).toSet
  }

  "MediaWiki" should {
    "get db name by host" in {
      MwDatabase.dbName("uk.wikipedia.org") === "ukwiki"
      MwDatabase.dbName("commons.wikimedia.org") === "commonswiki"
      MwDatabase.dbName("nl.wikimedia.org") === "nlwikimedia"
      MwDatabase.dbName("ru.wiktionary.org") === "ruwiktionary"
    }
  }
}
