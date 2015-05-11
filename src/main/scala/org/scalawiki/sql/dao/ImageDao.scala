package org.scalawiki.sql.dao

import org.scalawiki.sql.MediaWiki
import org.scalawiki.wlx.dto.Image

import scala.language.higherKinds
import scala.slick.driver.JdbcProfile

class ImageDao(val driver: JdbcProfile) {

  import driver.simple._

  val query = MediaWiki.images

  def insert(image: Image)(implicit session: Session) = {
      query += image
  }

  def list(implicit session: Session) = query.sortBy(_.name).run

  def get(name: String)(implicit session: Session): Option[Image] =
    query.filter(_.name === name).firstOption


}
