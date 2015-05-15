package org.scalawiki.query

import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.{Generator, PageIdsParam, TitlesParam}
import org.scalawiki.sql.dao.PageDao

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class DslQueryDbCache(val dslQuery: DslQuery) {

  val bot = dslQuery.bot

  var cacheStat: Option[CacheStat] = None

  def dbCache = bot.dbCache

  import bot.system.dispatcher
  import org.scalawiki.dto.cmd.query.Query

  import scala.slick.driver.H2Driver.simple._

  def run(): Future[Seq[Page]] = {

    val action = dslQuery.action
    action.query.map {
      query =>

        val hasContent = query.revisions.exists(_.hasContent)

        if (hasContent && dbCache) {
          implicit val session: Session = bot.session.get

          val mwDb = bot.database.get
          val pageDao = mwDb.pageDao

          val idsAction = Action(query.revisionsWithoutContent)

          val idsQuery = new DslQuery(idsAction, dslQuery.bot)
          idsQuery.run().map {
            pages =>

              val ids = pages.flatMap(_.id).toSet
              val dbPages = fromDb(pageDao, pages, ids)

              val notInDbPagesFuture = notInDb(query, ids, dbPages)

              val notInDbPages = Await.result(notInDbPagesFuture, 1.minute)
              toDb(pageDao, notInDbPages)

              cacheStat = Some(CacheStat(notInDbPages.size, dbPages.size))
              println(cacheStat)

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
        notInDBQuery(query, notInDbIds)
      }
      val notInDbQuery = new DslQuery(Action(notInDbQueryDto), dslQuery.bot)

      notInDbQuery.run()
    }
  }

  def notInDBQuery(query: Query, ids: Iterable[Long]): Query = {
    val params = query.params.filterNot { p =>
      p.isInstanceOf[Generator] ||
        p.isInstanceOf[TitlesParam] ||
        p.isInstanceOf[PageIdsParam]
    } :+ PageIdsParam(ids.toSeq)

    Query(params: _*)
  }

  def fromDb(pageDao: PageDao, pages: Seq[Page], ids: Set[Long])(implicit session: Session): Seq[Page] = {
    val revIds = pages.flatMap(_.revisions.headOption.flatMap(_.id))
    pageDao.findByRevIds(ids, revIds)
  }

  def toDb(pageDao: PageDao, pages: Seq[Page])(implicit session: Session) = {
    pages.map (pageDao.insert)
  }

}

case class CacheStat(newPages: Int, cached: Int)