package org.scalawiki.query

import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.{Generator, PageIdsParam, TitlesParam}
import org.scalawiki.sql.dao.PageDao

import scala.concurrent.Future

class DslQueryDbCache(val dslQuery: DslQuery) {

  val bot = dslQuery.bot

  var cacheStat: Option[CacheStat] = None

  def dbCache = bot.dbCache

  import bot.system.dispatcher
  import org.scalawiki.dto.cmd.query.Query

  def run(): Future[Seq[Page]] = {
    if (!dbCache) dslQuery.run()
    else {
      val action = dslQuery.action
      action.query.map {
        query =>

          val hasContent = query.revisions.exists(_.hasContent)

          if (hasContent && dbCache) {
            val mwDb = bot.mwDb.get
            val pageDao = mwDb.pageDao

            val idsAction = Action(query.revisionsWithoutContent)

            val idsQuery = new DslQuery(idsAction, dslQuery.bot)
            idsQuery.run().flatMap {
              pages =>

                val ids = pages.flatMap(_.id).toSet
                val dbPages = fromDb(pageDao, pages, ids)

                notInDb(query, ids, dbPages).map {
                  notInDbPages =>

                    if (notInDbPages.nonEmpty) {
                      toDb(pageDao, notInDbPages, dbPages)
                    }

                    cacheStat = Some(CacheStat(notInDbPages.size, dbPages.size))
                    bot.log.info(s"${bot.host} $cacheStat")

                    dbPages.filter(_.revisions.nonEmpty) ++ notInDbPages
                }
            }
          } else {
            dslQuery.run()
          }
      }.getOrElse(Future.successful(Seq.empty))
    }
  }

  def notInDb(query: Query, ids: Set[Long], dbPages: Seq[Page]): Future[Seq[Page]] = {
    val dbIds = dbPages.filter(_.revisions.nonEmpty).flatMap(_.id).toSet
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

  def fromDb(pageDao: PageDao, pages: Seq[Page], ids: Set[Long]): Seq[Page] = {
    val startTime = System.nanoTime()
    bot.log.info(s"${bot.host} query ${pages.size} pages from db")

    val revIds = pages.flatMap(_.revisions.headOption.flatMap(_.id))
    val pagesFromDb = pageDao.findByPageAndRevIdsOpt(ids, revIds)

    val estimatedTime = (System.nanoTime() - startTime) / Math.pow(10, 9)
    bot.log.info(s"${bot.host} Db query completed with ${pagesFromDb.size} pages in $estimatedTime seconds")
    pagesFromDb

  }

  def toDb(pageDao: PageDao, toDbPages: Seq[Page], inDbPages: Seq[Page]) = {
    val startTime = System.nanoTime()
    bot.log.info(s"${bot.host} insert ${toDbPages.size} pages to db")

    val inDbIds = inDbPages.map(_.id).toSet
    val (newRevPages, newPages) = toDbPages.partition(p => inDbIds.contains(p.id))

    if (newPages.nonEmpty) {
      pageDao.insertAll(newPages)
    }

    if (newRevPages.nonEmpty) {
      val revisionDao = bot.mwDb.get.revisionDao

      val newRevisions = newRevPages.map(_.revisions.head)
      revisionDao.insertAll(newRevisions)
      pageDao.updateLastRevision(newRevPages.map(_.id), newRevisions.map(_.id))
    }

    val estimatedTime = (System.nanoTime() - startTime) / Math.pow(10, 9)
    bot.log.info(s"${bot.host} Insert completed with ${toDbPages.size} pages in $estimatedTime seconds")
  }

}

case class CacheStat(newPages: Int, cached: Int)