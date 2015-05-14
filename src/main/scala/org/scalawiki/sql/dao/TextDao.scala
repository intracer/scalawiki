package org.scalawiki.sql.dao

import org.scalawiki.sql.{Text, Texts}

import scala.language.higherKinds
import scala.slick.driver.JdbcProfile
import scala.slick.lifted.TableQuery


class TextDao(val query: TableQuery[Texts], val driver: JdbcProfile) {

  import driver.simple._

  private val autoInc = query returning query.map(_.id)

  def insert(text: Text)(implicit session: Session): Option[Long] =
    autoInc += text

  def list(implicit session: Session) = query.run

}
