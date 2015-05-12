package org.scalawiki.sql


import org.specs2.mutable.{BeforeAfter, Specification}

import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable
import scala.slick.lifted.TableQuery

class SchemaSpec extends Specification with BeforeAfter {

  sequential

  implicit var session: Session = _

  def createSchema() = MediaWiki.createTables()

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

      import scala.slick.lifted.Tag
      val ukWikiPages = TableQuery[Pages]((tag: Tag) => new Pages(tag, Some("ukwiki")))

      MediaWiki.dropTables()
      MediaWiki.createIfNotExists(ukWikiPages)

      val names = getTableNames
      ukWikiPages.ddl.drop

      names === Set("ukwiki_page")
    }
   }

  def getTableNames: Set[String] = {
    val tables = MTable.getTables.list
    tables.map(_.name.name.toLowerCase).toSet
  }
}
