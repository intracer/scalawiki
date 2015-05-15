package org.scalawiki.sql


import org.specs2.mutable.{BeforeAfter, Specification}

import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable

class MwDatabaseSpec extends Specification with BeforeAfter {

  sequential

  implicit var session: Session = _

  var mwDb: MwDatabase = _

  def createSchema() = mwDb.createTables()

  override def before = {
    // session = Database.forURL("jdbc:h2:~/test", driver = "org.h2.Driver").createSession()
    session = Database.forURL("jdbc:h2:mem:test", driver = "org.h2.Driver").createSession()
    mwDb = new MwDatabase(session)
  }

  override def after = session.close()

  val tableNames = Set("category",
    "image",
    "page",
    "revision",
    "text",
    "user")

  "ddls" should {
    "create schema" in {
      new MwDatabase(session, Some("ukwiki")).dropTables() // hack
      createSchema()

      getTableNames === tableNames
    }

    "create database with one custom prefix" in {
      val mwDbCustom = new MwDatabase(session, Some("ukwiki"))

      mwDb.dropTables()
      mwDbCustom.createTables()

      val names = getTableNames

      mwDbCustom.dropTables()

      names === tableNames.map("ukwiki_" + _)
      getTableNames.isEmpty === true
    }

    "create database with several custom prefix" in {

      val prefixes = Seq("ukwiki", "commons", "enwiki")
      val dbs = prefixes.map(name => new MwDatabase(session, Some(name)))

      mwDb.dropTables()

      dbs.foreach(_.createTables())

      val names = getTableNames.toSet

      dbs.foreach(_.dropTables())

      val expectedNames = prefixes.flatMap(prefix => tableNames.map(prefix + "_" + _)).toSet
      names === expectedNames

      getTableNames.isEmpty === true
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
}
