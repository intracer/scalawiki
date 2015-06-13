package org.scalawiki.sql.dao

import org.scalawiki.sql.{MwDatabase, Text, Texts}

import scala.language.higherKinds
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import spray.util.pimpFuture

class TextDao(val mwDb: MwDatabase, val query: TableQuery[Texts], val driver: JdbcProfile) {

  import driver.api._

  val db = mwDb.db

  private val autoInc = query returning query.map(_.id)

  def insert(text: Text): Option[Long] =
    db.run(autoInc += text).await

  def list = db.run(query.result).await

}
