package org.scalawiki.sql.dao

import org.scalawiki.dto.User
import org.scalawiki.sql.{MwDatabase, Users}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import spray.util.pimpFuture

import scala.language.higherKinds

class UserDao(
    val mwDb: MwDatabase,
    val query: TableQuery[Users],
    val driver: JdbcProfile
) {

  import driver.api._

  private val autoInc = query returning query.map(_.id)

  val db = mwDb.db

  def insertAll(users: Iterable[User]): Unit = {
    db.run(query.forceInsertAll(users)).await
  }

  def insert(user: User): Option[Long] = {
    if (user.id.isDefined) {
      db.run(query.forceInsert(user)).await
      user.id
    } else {
      db.run(autoInc += user).await
    }
  }

  def list = db.run(query.sortBy(_.id).result).await

  def count = db.run(query.length.result).await

  def find(ids: Iterable[Long]): Seq[User] =
    db.run(query.filter(_.id inSet ids).sortBy(_.id).result).await

  def get(id: Long): Option[User] =
    db.run(query.filter(_.id === id).result.headOption).await

  def get(name: String): Option[User] =
    db.run(query.filter(_.name === name).result.headOption).await

}
