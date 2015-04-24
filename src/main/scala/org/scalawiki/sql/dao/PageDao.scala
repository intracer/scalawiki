package org.scalawiki.sql.dao

import org.scalawiki.dto.{Revision, Page}
import org.scalawiki.sql.MediaWiki

import scala.language.higherKinds
import scala.slick.driver.JdbcProfile

class PageDao(val driver: JdbcProfile) {

  import driver.simple._

  val query = MediaWiki.pages

  import MediaWiki.{pages, revisions, texts}

  val revisionDao = new RevisionDao(driver)

  private val autoInc = query returning query.map(_.id)

  def insert(page: Page)(implicit session: Session): Option[Long] = {

    val lastRevOpt = page.revisions.headOption
    require(lastRevOpt.isDefined, "page has no revisions")
    val lastRev = lastRevOpt.get

    val pageId = autoInc += page

    if (lastRev.id.isEmpty) {
      val withPage: Revision = lastRev.copy(pageId = pageId)
      val revId = revisionDao.insert(withPage)

      pages.filter(_.id === pageId)
        .map(p => p.pageLatest)
        .update(revId.get)

    }
    pageId
  }

  def list(implicit session: Session) = query.run

  def get(id: Long)(implicit session: Session): Option[Page] = query.filter {
    _.id === id
  }.firstOption

  def withText(id: Long)(implicit session: Session): Option[Page] =
    (pages.filter {
      _.id === id
    } join revisions on (_.pageLatest === _.id) join texts on (_._2.textId === _.id)).run.map {
      case ((p, r), t) => p.copy(revisions = Seq(r.copy(content = Some(t.text))))
    }.headOption


}
