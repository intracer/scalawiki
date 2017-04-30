package org.scalawiki.sql

import org.scalawiki.{MwBot, MwBotImpl}
import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.{Generator, PageIdsParam, TitlesParam}
import org.scalawiki.query.DslQuery

import scala.concurrent.Future

class DbCachedBot(apiBot: MwBot, database: MwDatabase)
  extends MwBotImpl(apiBot.host, apiBot.asInstanceOf[MwBotImpl].http) {

  override def run(action: Action, context: Map[String, String] = Map.empty): Future[Seq[Page]] = {
    new DslQueryDbCache(new DslQuery(action, apiBot), database).run()
  }
}

case class CacheStat(newPages: Int, cached: Int)

class DslQueryDbCache(val dslQuery: DslQuery, val database: MwDatabase) {

  import scala.concurrent.ExecutionContext.Implicits.global
  import org.scalawiki.dto.cmd.query.Query

  val bot = dslQuery.bot
  val log = bot.log

  var cacheStat: Option[CacheStat] = None

  val pageDao = database.pageDao

  def run(): Future[Seq[Page]] = {
    dslQuery.action.query.map {
      query =>
        if (cachingProvided(query)) {
          runWithCaching(query)
        } else {
          dslQuery.run()
        }
    }.getOrElse(Future.successful(Seq.empty))
  }

  def cachingProvided(query: Query): Boolean = query.revisions.exists(_.hasContent)

  def runWithCaching(query: Query): Future[Seq[Page]] = {

    idsOnlyApiQuery(query).run().flatMap {
      idsOnlyPages =>

        val pageIds = idsOnlyPages.flatMap(_.id).toSet
        val revIds = idsOnlyPages.flatMap(_.revisions.headOption.flatMap(_.id)).toSet

        checkUnusualCases(idsOnlyPages)

        val dbPages = fromDb(revIds, pageIds)

        notInDb(query, pageIds, dbPages).map {
          mergePages(dbPages, _)
        }
    }
  }

  def checkUnusualCases(pages: Seq[Page]): Unit = {
    val noRevs = pages.filter(p => p.revisions.isEmpty || p.revisions.head.id.isEmpty)
    if (noRevs.nonEmpty) {
      log.error("No revs pages" + noRevs.toBuffer)
    }
  }

  def idsOnlyApiQuery(query: Query): DslQuery = {
    val idsAction = Action(query.revisionsWithoutContent)

    new DslQuery(idsAction, dslQuery.bot)
  }

  def mergePages(dbPages: Seq[Page], notInDbPages: Seq[Page]): Seq[Page] = {
    if (notInDbPages.nonEmpty) {
      val needNewRevPageIds = dbPages.filter(_.revisions.isEmpty).flatMap(_.id).toSet

      val duplicates = notInDbPages.groupBy(_.id.get).values.filter(_.size > 1)
      if (duplicates.nonEmpty) {
        log.error("Duplicates present from API" + duplicates.toBuffer)
      }

      toDb(notInDbPages, needNewRevPageIds)
    }

    cacheStat = Some(CacheStat(notInDbPages.size, dbPages.size))
    log.info(s"${bot.host} $cacheStat")

    dbPages.filter(_.revisions.nonEmpty) ++ notInDbPages
  }

  def notInDb(query: Query, ids: Set[Long], dbPages: Seq[Page]): Future[Seq[Page]] = {
    val dbIds = dbPages.filter(_.revisions.nonEmpty).flatMap(_.id).toSet
    val notInDbIds = ids -- dbIds

    //    log.info(s"fully in db pageIds: $dbIds")
    //    log.info(s"not in DB (fully or rev missing) pageIds: $notInDbIds")

    if (notInDbIds.isEmpty) {
      Future.successful(Seq.empty)
    } else {
      val notInDbQueryDtos = if (dbIds.isEmpty) {
        Seq(query)
      } else {
        notInDBQuery(query, notInDbIds.toSeq.sorted)
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

  def fromDb(revIds: Set[Long], ids: Set[Long]): Seq[Page] = {
    val startTime = System.nanoTime()
    log.info(s"${bot.host} query ${ids.size} pages from db")

    if (revIds.size != ids.size) {
      log.error(s"pageIds.size ${ids.size}, revIds.size: ${revIds.size}")
    }

    val pagesFromDb = pageDao.listWithText.filter(p => ids.contains(p.id.get)).map {
      p =>
        if (revIds.contains(p.revisions.head.id.get)) p else p.copy(revisions = Seq.empty)
    }

    //  log.info(s"pagesFromDb pageIds ${pagesFromDb.flatMap(_.id)}")

    val estimatedTime = (System.nanoTime() - startTime) / Math.pow(10, 9)
    log.info(s"${bot.host} Db query completed with ${pagesFromDb.size} pages in $estimatedTime seconds")
    pagesFromDb
  }

  /**
    *
    * @param toDbPages         pages to save in Db. Can be either new pages or new page revisions
    * @param needNewRevPageIds ids of pages that are already in db but have new revisions to add
    */
  def toDb(toDbPages: Seq[Page], needNewRevPageIds: Set[Long]) = {
    val startTime = System.nanoTime()
    log.info(s"${bot.host} insert ${toDbPages.size} pages to db")

    //    log.info(s"toDbPages pageIds: ${toDbPages.flatMap(_.id).toBuffer}")
    //    log.info(s"old rev inDb pageIds: $inDbPageIds")


    val (newRevPages, newPages) = toDbPages.partition(p => needNewRevPageIds.contains(p.id.get))

    //    log.info(s"newPages pageIds: ${newPages.flatMap(_.id).toBuffer}")
    //    log.info(s"newRevPages pageIds: ${newRevPages.flatMap(_.id).toBuffer}")

    if (newPages.nonEmpty) {

      val duplicates = newPages.groupBy(_.id.get).values.filter(_.size > 1)
      if (duplicates.nonEmpty) {
        log.error("Duplicates present to DB" + duplicates.toBuffer)
      }

      pageDao.insertAll(newPages)
    }

    if (newRevPages.nonEmpty) {
      val revisionDao = database.revisionDao

      val newRevisions = newRevPages.map(_.revisions.head)
      revisionDao.insertAll(newRevisions)
      pageDao.updateLastRevision(newRevPages.map(_.id), newRevisions.map(_.id))
    }

    val estimatedTime = (System.nanoTime() - startTime) / Math.pow(10, 9)
    log.info(s"${bot.host} Insert completed with ${toDbPages.size} pages in $estimatedTime seconds")
  }
}
