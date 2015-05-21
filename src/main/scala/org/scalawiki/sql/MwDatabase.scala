package org.scalawiki.sql

import org.scalawiki.sql.dao._
import slick.driver.H2Driver.api._
import slick.driver.{H2Driver, JdbcProfile}
import slick.jdbc.meta.MTable
import slick.lifted.TableQuery
import scala.concurrent.ExecutionContext.Implicits.global

class MwDatabase(val db: Database,
                 val dbName: Option[String] = None,
                 val driver: JdbcProfile = H2Driver) {

  def prefixed(tableName: String) = dbName.fold("")(_ + "_") + tableName

  val categories = TableQuery[Categories](
    (tag: Tag) => new Categories(tag, prefixed("category")))

  val images = TableQuery[Images](
    (tag: Tag) => new Images(tag, prefixed("image")))

  val pages = TableQuery[Pages](
    (tag: Tag) => new Pages(tag, prefixed("page"), dbName))

  val revisions = TableQuery[Revisions](
    (tag: Tag) => new Revisions(tag, prefixed("revision"), dbName))

  val texts = TableQuery[Texts](
    (tag: Tag) => new Texts(tag, prefixed("text")))

  val users = TableQuery[Users](
    (tag: Tag) => new Users(tag, prefixed("user"), dbName))

  val imageDao = new ImageDao(images, driver)
  val textDao = new TextDao(texts, driver)
  val userDao = new UserDao(users, driver)
  val revisionDao = new RevisionDao(this, driver)
  val pageDao = new PageDao(this, driver)

  def tables =
    Seq(
      users,
      categories,
      images,
      revisions,
      pages,
      texts
    )

  def createTables() {
    createIfNotExists(tables: _*)
  }

  def dropTables() {
    tables.foreach { table =>
      db.run(MTable.getTables(table.baseTableRow.tableName)).map { result =>
        if (result.nonEmpty) {
          db.run(table.schema.drop)
        }
      }
    }
  }

  def createIfNotExists(tables: TableQuery[_ <: Table[_]]*) {
    tables foreach { table =>
      db.run(MTable.getTables(table.baseTableRow.tableName)).map { result =>
        if (result.isEmpty) {
          db.run(table.schema.create)
        }
      }
    }
  }
}

object MwDatabase {

  def dbName(host: String): String = {
    host.split("\\.").toList match {
      case "commons" :: "wikimedia" :: xs => "commonswiki"
      case x :: "wikipedia" :: xs => x + "wiki"
      case x1 :: x2 :: xs => x1 + x2
    }
  }
}
