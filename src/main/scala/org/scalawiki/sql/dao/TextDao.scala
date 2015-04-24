package org.scalawiki.sql.dao

import org.scalawiki.sql.{MediaWiki, Text}

import scala.language.higherKinds
import scala.slick.driver.JdbcProfile


class TextDao(val driver: JdbcProfile) {

  import driver.simple._

  val query = MediaWiki.texts

  private val autoInc = query returning query.map(_.id)

  def insert(text: Text)(implicit session: Session): Option[Long] =
    autoInc += text

  def list(implicit session: Session) = query.run


}
