package org.scalawiki.sql.dao

import org.scalawiki.dto.Revision
import org.scalawiki.sql.{MediaWiki, Text}

import scala.language.higherKinds
import scala.slick.driver.JdbcProfile


class RevisionDao(val driver: JdbcProfile) {

  import MediaWiki.{revisions, texts}
  import driver.simple._

  val query = MediaWiki.revisions

  val textDao = new TextDao(driver)

  private val autoInc = query returning query.map(_.id)

  def insert(revision: Revision)(implicit session: Session): Option[Long] = {
    val revId = if (revision.id.isDefined) {
      if (get(revision.id.get).isEmpty) {
        query.forceInsert(addText(revision))
      }

      revision.id
    }
    else {
      autoInc += addText(revision)
    }
    revId
  }

  def addText(revision: Revision)(implicit session: Session): Revision = {
    val text = Text(None, revision.content.getOrElse(""))
    //    revision.textId = textId
    val textId = textDao.insert(text)
    val withText: Revision = revision.copy(textId = textId)
    withText
  }

  def list(implicit session: Session) = query.run

  def get(id: Long)(implicit session: Session): Option[Revision] = query.filter {
    _.id === id
  }.firstOption

  def withText(id: Long)(implicit session: Session): Option[Revision] =
    (revisions.filter {
      _.id === id
    } join texts on (_.textId === _.id)).run.map {
      case (r, t) => r.copy(content = Some(t.text))
    }.headOption
}
