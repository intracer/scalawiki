package org.scalawiki.sql.dao

import org.scalawiki.sql.{Text, Texts}

import scala.language.higherKinds
import slick.driver.JdbcProfile
import slick.lifted.TableQuery


class TextDao(val query: TableQuery[Texts], val driver: JdbcProfile) {

  import driver.api._

  private val autoInc = query returning query.map(_.id)

  def insert(text: Text)(implicit session: Session): Option[Long] =
    autoInc += text

  def list(implicit session: Session) = query.run

}
