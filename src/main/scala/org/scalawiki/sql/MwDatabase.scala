package org.scalawiki.sql

import org.scalawiki.sql.dao._

import scala.slick.driver.H2Driver.simple._
import scala.slick.driver.{H2Driver, JdbcProfile}
import scala.slick.jdbc.meta.MTable
import scala.slick.lifted.TableQuery

class MwDatabase(val session: Session,
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

  def createTables()(implicit session: Session) {
    //dropTables()
    createIfNotExists(tables: _*)
  }

  def dropTables()(implicit session: Session) {
    tables foreach { table =>
      if (MTable.getTables(table.baseTableRow.tableName).list.nonEmpty) {
        table.ddl.drop
      }
    }
  }

  def createIfNotExists(tables: TableQuery[_ <: Table[_]]*)(implicit session: Session) {
    tables foreach { table =>
      if (MTable.getTables(table.baseTableRow.tableName).list.isEmpty) {
        table.ddl.create
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
