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

    require(page.revisions.nonEmpty, "page has no revisions")

    val pageId = if (page.id.isDefined) {
      pages.forceInsert(page)
      page.id
    }
    else {
      autoInc += page
    }

    val newRevs = page.revisions //.filter(_.revId.isEmpty)

    addRevisions(pageId.get, newRevs)

    pageId
  }

  def addRevisions(pageId: Long, newRevs: Seq[Revision])(implicit session: Session) = {
    val revIds = newRevs.reverse.flatMap { rev =>
      val withPage = rev.copy(pageId = Some(pageId))
      revisionDao.insert(withPage)
    }

    pages.filter(_.id === pageId)
      .map(p => p.pageLatest)
      .update(revIds.last)
  }

  def list(implicit session: Session) = query.sortBy(_.id).run

  def get(id: Long)(implicit session: Session): Option[Page] =
    pages.filter(_.id === id).firstOption

  def find(ids: Iterable[Long])(implicit session: Session): Seq[Page] =
    pages.filter(_.id inSet ids).sortBy(_.id).run

  def findWithText(ids: Iterable[Long])(implicit session: Session): Seq[Page] =
    (pages.filter(_.id inSet ids)
      join revisions on (_.pageLatest === _.id)
      join texts on (_._2.textId === _.id)
      ).sortBy { case ((p, r), t) => p.id }.run.map {
      case ((p, r), t) => p.copy(revisions = Seq(r.copy(content = Some(t.text))))
    }

  def findByRevIds(ids: Iterable[Long], revIds: Iterable[Long])(implicit session: Session): Seq[Page] =
    (pages.filter(_.id inSet ids)
      join revisions.filter(_.id inSet revIds) on (_.id === _.pageId)
      join texts on (_._2.textId === _.id)
      ).sortBy { case ((p, r), t) => p.id }.run.map {
      case ((p, r), t) => p.copy(revisions = Seq(r.copy(content = Some(t.text))))
    }

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
