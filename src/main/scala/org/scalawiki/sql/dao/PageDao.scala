package org.scalawiki.sql.dao

import org.scalawiki.dto.{Page, Revision}
import org.scalawiki.sql.MwDatabase
import org.scalawiki.wlx.dto.Image

import scala.language.higherKinds
import scala.slick.driver.JdbcProfile
import scala.util.Try
import scala.util.control.NonFatal

class PageDao(val mwDb: MwDatabase, val driver: JdbcProfile) {

  import driver.simple._

  val pages = mwDb.pages
  val revisions = mwDb.revisions
  val texts = mwDb.texts
  val images = mwDb.images

  val revisionDao = mwDb.revisionDao
  val imageDao = mwDb.imageDao

  private val autoInc = pages returning pages.map(_.id)

  def insertAll(pageSeq: Seq[Page])(implicit session: Session): Unit = {
    pages.forceInsertAll(pageSeq: _*)

    revisionDao.insertAll(pageSeq.flatMap(_.revisions.headOption))
  }

  def insert(page: Page)(implicit session: Session): Try[Long] = {

    val result = Try {
      require(page.revisions.nonEmpty, "page has no revisions")

      val pageId = if (page.id.isDefined) {
        if (get(page.id.get).isEmpty) {
          pages.forceInsert(page)
        }
        page.id
      }
      else {
        autoInc += page
      }

      val newRevs = page.revisions //.filter(_.revId.isEmpty)

      addRevisions(pageId.get, newRevs)
      addImages(pageId.get, page.images)

      pageId.get
    }

    result.recover { case NonFatal(e) => println(s"error saving page $page $e") }

    result
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

  def addImages(pageId: Long, images: Seq[Image])(implicit session: Session) = {
    images.reverse.foreach { image =>
      val withPage = image.copy(pageId = Some(pageId))
      imageDao.insert(withPage)
    }
  }

  def list(implicit session: Session) = pages.sortBy(_.id).run

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

  def findByRevIds(ids: Iterable[Long], revIds: Iterable[Long])(implicit session: Session): Seq[Page] = {
    ((for {
      p <- pages if p.id inSet ids
      r <- revisions if r.pageId === p.id
      t <- texts if r.textId === t.id
      i <- images if i.pageId === p.id
    } yield (p, r, t, i)
      ) sortBy { case (p, r, t, i) => r.id.desc }
      ).run.map {
      case (p, r, t, i) => p.copy(revisions = Seq(r.copy(content = Some(t.text))), images = Seq(i))
    }
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
