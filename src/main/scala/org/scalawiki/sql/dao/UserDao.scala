package org.scalawiki.sql.dao

import org.scalawiki.dto.User
import org.scalawiki.sql.MediaWiki

import scala.language.higherKinds
import scala.slick.driver.JdbcProfile


class UserDao(val driver: JdbcProfile) {

  import driver.simple._

  val query = MediaWiki.users

  private val autoInc = query returning query.map(_.id)

  def insert(user: User)(implicit session: Session): Option[Long] =
    autoInc += user

  def list(implicit session: Session) = query.run

  def get(id: Long)(implicit session: Session): Option[User] = query.filter { _.id === id }.firstOption

}
