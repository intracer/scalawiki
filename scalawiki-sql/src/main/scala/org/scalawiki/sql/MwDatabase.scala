package org.scalawiki.sql

import org.scalawiki.sql.dao._
import slick.driver.H2Driver.api._
import slick.driver.{H2Driver, JdbcProfile}
import slick.jdbc.meta.MTable
import slick.lifted.TableQuery
import spray.util.pimpFuture

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

  val imageDao = new ImageDao(this, images, driver)
  val textDao = new TextDao(this, texts, driver)
  val userDao = new UserDao(this, users, driver)
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
    createIfNotExists()
  }

  def existingTables(existing: Boolean) = {
    db.run(MTable.getTables).map {
      dbTables =>
        val dbTableNames = dbTables.map(_.name.name).toSet
        tables.filter(t => {
          val tableName = t.baseTableRow.tableName
          val contains = dbTableNames.contains(tableName)
          if (existing)
            contains
          else
            !contains
        })
    }
  }

  def dropTables() {
    val toDrop = existingTables(true).await
    db.run(
      DBIO.sequence(toDrop.map(_.schema.drop))
    ).await
  }

  def createIfNotExists() {
    val toCreate = existingTables(false).await
    db.run(
      DBIO.sequence(toCreate.map(_.schema.create))
    ).await
  }
}

object MwDatabase {

  def create(host: String) = {
    val db = Database.forURL("jdbc:h2:~/scalawiki", driver = "org.h2.Driver")
    val database = new MwDatabase(db, Some(MwDatabase.dbName(host)))
    database.createTables()
    database
  }

  def dbName(host: String): String = {
    host.split("\\.").toList match {
      case "commons" :: "wikimedia" :: xs => "commonswiki"
      case x :: "wikipedia" :: xs => x + "wiki"
      case x1 :: x2 :: xs => x1 + x2
    }
  }
}
