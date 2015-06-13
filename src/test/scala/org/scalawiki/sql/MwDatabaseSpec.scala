package org.scalawiki.sql


import slick.driver.JdbcProfile
import slick.backend.DatabaseConfig
import org.specs2.mutable.{BeforeAfter, Specification}
import slick.jdbc.meta.MTable
import spray.util.pimpFuture

class MwDatabaseSpec extends Specification with BeforeAfter {

  sequential


  var mwDb: MwDatabase = _
  var dc: DatabaseConfig[JdbcProfile] = _

  def createSchema() = mwDb.createTables()

  override def before = {
    dc = DatabaseConfig.forConfig[JdbcProfile]("h2mem")
    mwDb = new MwDatabase(dc.db)
  }

  override def after = mwDb.db.close()

  val tableNames = Set("category",
    "image",
    "page",
    "revision",
    "text",
    "user")

  "ddls" should {
    "create schema" in {
      new MwDatabase(dc.db, Some("ukwiki")).dropTables() // hack
      createSchema()

      getTableNames === tableNames
    }

    "create database with one custom prefix" in {
      val mwDbCustom = new MwDatabase(dc.db, Some("ukwiki"))

      mwDb.dropTables()
      mwDbCustom.createTables()

      val names = getTableNames

      mwDbCustom.dropTables()

      names === tableNames.map("ukwiki_" + _)
      getTableNames.isEmpty === true
    }

    "create database with several custom prefix" in {

      val prefixes = Seq("ukwiki", "commons", "enwiki")
      val dbs = prefixes.map(name => new MwDatabase(dc.db, Some(name)))

      mwDb.dropTables()

      dbs.foreach(_.createTables())

      val names = getTableNames.toSet

      dbs.foreach(_.dropTables())

      val expectedNames = prefixes.flatMap(prefix => tableNames.map(prefix + "_" + _)).toSet
      names === expectedNames

      getTableNames.isEmpty === true
    }

    def getTableNames: Set[String] = {
      val tables = mwDb.db.run(MTable.getTables).await
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
