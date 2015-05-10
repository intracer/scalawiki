package org.scalawiki.query

import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.{Generator, PageIdsParam, Query, TitlesParam}
import org.scalawiki.sql.dao.PageDao

import scala.concurrent.Future
import scala.slick.driver.H2Driver

class DslQueryDbCache(val dslQuery: DslQuery, dbCache: Boolean = true) {

  import dslQuery.bot.system.dispatcher

  def run(): Future[Seq[Page]] = {

    val action = dslQuery.action
    action.query.map {
      query =>

        val hasContent = query.revisions.exists(_.hasContent)

        if (hasContent && dbCache) {
          val idsAction = Action(query.revisionsWithoutContent)

          val idsQuery = new DslQuery(idsAction, dslQuery.bot)
          idsQuery.run().flatMap {
            pages =>

              val ids = pages.flatMap(_.id).toSet
              val dbPages = fromDb(pages, ids)

              val notInDbPages = notInDb(query, ids, dbPages)

              toDb(notInDbPages)

              notInDbPages
          }
        } else {
          dslQuery.run()
        }
    }.getOrElse(Future.successful(Seq.empty))
  }

  def notInDb(query: Query, ids: Set[Long], dbPages: Seq[Page]): Future[Seq[Page]] = {
    val dbIds = dbPages.flatMap(_.id).toSet
    val notInDbIds = ids -- dbIds

    if (notInDbIds.isEmpty) {
      Future.successful(dbPages)
    } else {
      val notInDbQueryDto = if (dbIds.isEmpty) {
        query
      } else {
        DslQueryDbCache.notInDBQuery(query, notInDbIds)
      }
      val notInDbQuery = new DslQuery(Action(notInDbQueryDto), dslQuery.bot)

      notInDbQuery.run()
    }
  }

  def fromDb(pages: Seq[Page], ids: Set[Long]): Seq[Page] = {
    implicit val session = DslQueryDbCache.session
    val revIds = pages.flatMap(_.revisions.headOption.flatMap(_.id))
    DslQueryDbCache.pageDao.findByRevIds(ids, revIds)
  }

  def toDb(pagesFuture: Future[Seq[Page]]) = {
    implicit val session = DslQueryDbCache.session
    pagesFuture.map { pages =>
      pages.map { page =>
        DslQueryDbCache.pageDao.addRevisions(page.id.get, page.revisions.headOption.toSeq)
      }
    }
  }

}

object DslQueryDbCache {

  import org.scalawiki.dto.cmd.query.Query

  import scala.slick.driver.H2Driver.simple._

  val session = Database.forURL("jdbc:h2:~/scalawiki", driver = "org.h2.Driver").createSession()

  val pageDao = new PageDao(H2Driver)

  def notInDBQuery(query: Query, ids: Iterable[Long]): Query = {
    val params = query.params.filterNot { p =>
      p.isInstanceOf[Generator] ||
        p.isInstanceOf[TitlesParam] ||
        p.isInstanceOf[PageIdsParam]
    } :+ PageIdsParam(ids.toSeq)

    Query(params: _*)
  }
}