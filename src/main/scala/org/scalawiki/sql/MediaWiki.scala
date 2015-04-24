package org.scalawiki.sql

import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable
import scala.slick.lifted.TableQuery

object MediaWiki {

  lazy val categories = TableQuery[Categories]
  lazy val images = TableQuery[Images]
  lazy val pages = TableQuery[Pages]
  lazy val revisions = TableQuery[Revisions]
  lazy val texts = TableQuery[Texts]
  lazy val users = TableQuery[Users]

  def tables =
    Seq(
      users,
      categories,
      images,
      revisions,
      pages,
      texts
      )

  def createTables()(implicit session: Session) = createIfNotExists(tables: _*)

  def createIfNotExists(tables: TableQuery[_ <: Table[_]]*)(implicit session: Session) {
    tables foreach { table =>
      if (MTable.getTables(table.baseTableRow.tableName).list.isEmpty)
        table.ddl.create
    }
  }

}
