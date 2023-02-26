package org.scalawiki.sql.dao

import org.scalawiki.sql.{Images, MwDatabase}
import org.scalawiki.dto.Image
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import spray.util.pimpFuture

import scala.language.higherKinds

class ImageDao(val mwDb: MwDatabase, val query: TableQuery[Images], val driver: JdbcProfile) {

  import driver.api._

  val db = mwDb.db

  def insert(image: Image): Long = {
    db.run(query += image).await
  }

  def insertAll(images: Iterable[Image]): Unit = {
    db.run(query.forceInsertAll(images)).await
  }

  def list: Seq[Image] = db.run(query.sortBy(_.name).result).await

  def get(name: String): Option[Image] =
    db.run(query.filter(_.name === name).result.headOption).await

}

