package org.scalawiki.query

import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.{Generator, PageIdsParam, Query, TitlesParam}
import org.scalawiki.sql.MwDatabase

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

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
          idsQuery.run().map {
            pages =>

              val ids = pages.flatMap(_.id).toSet
              val dbPages = DslQueryDbCache.fromDb(pages, ids)

              val notInDbPagesFuture = notInDb(query, ids, dbPages)

              val notInDbPages = Await.result(notInDbPagesFuture, 1.minute)
              DslQueryDbCache.toDb(notInDbPages)

              dbPages ++ notInDbPages
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
      Future.successful(Seq.empty)
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

}

object DslQueryDbCache {

  import org.scalawiki.dto.cmd.query.Query

  import scala.slick.driver.H2Driver.simple._

  implicit var session: Session = _

  val mwDb = new MwDatabase()
  val pageDao = mwDb.pageDao

  def notInDBQuery(query: Query, ids: Iterable[Long]): Query = {
    val params = query.params.filterNot { p =>
      p.isInstanceOf[Generator] ||
        p.isInstanceOf[TitlesParam] ||
        p.isInstanceOf[PageIdsParam]
    } :+ PageIdsParam(ids.toSeq)

    Query(params: _*)
  }

  def fromDb(pages: Seq[Page], ids: Set[Long]): Seq[Page] = {
    val revIds = pages.flatMap(_.revisions.headOption.flatMap(_.id))
    pageDao.findByRevIds(ids, revIds)
  }

  def toDb(pages: Seq[Page]) = {
    pages.map { page =>
      pageDao.insert(page)
    }
  }

}