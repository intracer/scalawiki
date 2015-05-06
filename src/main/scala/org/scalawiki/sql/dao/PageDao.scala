package org.scalawiki.sql.dao

import org.scalawiki.dto.Page
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

    require(page.revisions.nonEmpty, "page has no revisions")

    val pageId = autoInc += page

    val newRevs = page.revisions.filter(_.revId.isEmpty)
    val revIds = newRevs.reverse.flatMap { rev =>
      val withPage = rev.copy(pageId = pageId)
      revisionDao.insert(withPage)
    }

    pages.filter(_.id === pageId)
      .map(p => p.pageLatest)
      .update(revIds.last)

    pageId
  }

  def list(implicit session: Session) = query.run

  def get(id: Long)(implicit session: Session): Option[Page] =
    query.filter(_.id === id).firstOption

  def withText(id: Long)(implicit session: Session): Option[Page] =
    (pages.filter(_.id === id)
      join revisions on (_.pageLatest === _.id)
      join texts on (_._2.textId === _.id)
      ).run.map {
      case ((p, r), t) => p.copy(revisions = Seq(r.copy(content = Some(t.text))))
    }.headOption

  def withRevisions(id: Long)(implicit session: Session): Option[Page] = {
    val rows = ((
      for {
        p <- pages if p.id === id
        r <- revisions if r.pageId === p.id
        t <- texts if r.textId === t.id
      } yield (p, r, t)
      ) sortBy { case (p, r, t) => r.id.desc }
      ).run.map {
      case (p, r, t) => (p, r.copy(content = Some(t.text)))
    }

    val revs = rows.map { case (p, r) => r }
    rows.headOption.map { case (p, r) => p.copy(revisions = revs) }
  }


}
