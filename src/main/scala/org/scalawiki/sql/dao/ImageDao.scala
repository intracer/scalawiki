package org.scalawiki.sql.dao

import org.scalawiki.sql.Images
import org.scalawiki.wlx.dto.Image
import slick.driver.JdbcProfile
import slick.lifted.TableQuery

import scala.language.higherKinds

class ImageDao(val query: TableQuery[Images], val driver: JdbcProfile) {

  import driver.api._

  def insert(image: Image) = {
    query += image
  }

  def list = query.sortBy(_.name)

  def get(name: String) =
    query.filter(_.name === name).result.headOption

}

