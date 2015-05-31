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
    if (!dbCache) dslQuery.run()
    else {
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
                bot.log.info(s"${bot.host} $cacheStat")

                dbPages ++ notInDbPages
            }
          } else {
            dslQuery.run()
          }
      }.getOrElse(Future.successful(Seq.empty))
    }
  }

  def notInDb(query: Query, ids: Set[Long], dbPages: Seq[Page]): Future[Seq[Page]] = {
    val dbIds = dbPages.flatMap(_.id).toSet
    val notInDbIds = ids -- dbIds

    if (notInDbIds.isEmpty) {
      Future.successful(Seq.empty)
    } else {
      val notInDbQueryDtos = if (dbIds.isEmpty) {
        Seq(query)
      } else {
        notInDBQuery(query, notInDbIds)
      }
      val notInDbQueries = notInDbQueryDtos.map(dto => new DslQuery(Action(dto), dslQuery.bot))

      Future.traverse(notInDbQueries)(_.run()).map(seqs => seqs.flatten)
    }
  }

  def notInDBQuery(query: Query, ids: Iterable[Long]): Seq[Query] = {
    ids.sliding(50, 50).map { window =>
      val params = query.params.filterNot { p =>
        p.isInstanceOf[Generator] ||
          p.isInstanceOf[TitlesParam] ||
          p.isInstanceOf[PageIdsParam]
      } :+ PageIdsParam(window.toSeq)

      Query(params: _*)
    }.toSeq
  }

  def fromDb(pageDao: PageDao, pages: Seq[Page], ids: Set[Long])(implicit session: Session): Seq[Page] = {
    val startTime = System.nanoTime()
    bot.log.info(s"${bot.host} query ${pages.size} pages from db")

    val revIds = pages.flatMap(_.revisions.headOption.flatMap(_.id))
    val pagesFromDb = pageDao.findByRevIds(ids, revIds)

    val estimatedTime = (System.nanoTime() - startTime) / Math.pow(10, 9)
    bot.log.info(s"${bot.host} Db query completed with ${pagesFromDb.size} pages in $estimatedTime seconds")
    pagesFromDb
  }

  def toDb(pageDao: PageDao, pages: Seq[Page])(implicit session: Session) = {
    val startTime = System.nanoTime()
    bot.log.info(s"${bot.host} insert ${pages.size} pages to db")

    pages.map(pageDao.insert)

    val estimatedTime = (System.nanoTime() - startTime) / Math.pow(10, 9)
    bot.log.info(s"${bot.host} Insert completed with ${pages.size} pages in $estimatedTime seconds")
  }

}

case class CacheStat(newPages: Int, cached: Int)