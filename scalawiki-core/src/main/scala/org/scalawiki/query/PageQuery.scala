package org.scalawiki.query

import org.scalawiki.MwBot
import org.scalawiki.dto.Page

import scala.concurrent.Future

trait PageQuery {

  def revisions(namespaces: Set[Int] = Set.empty, props: Set[String] = Set.empty, continueParam: Option[(String, String)] = None): Future[Seq[Page]]

}


object PageQuery {

  def byTitles(titles: Set[String], bot: MwBot): PageQuery = new PageQueryImplDsl(Right(titles), bot, bot.dbCache)

  def byTitle(title: String, bot: MwBot): SinglePageQuery = new PageQueryImplDsl(Right(Set(title)), bot, bot.dbCache)

  def byIds(ids: Set[Long], bot: MwBot):PageQuery = new PageQueryImplDsl(Left(ids), bot, bot.dbCache)

  def byId(id: Long, bot: MwBot): SinglePageQuery = new PageQueryImplDsl(Left(Set(id)), bot, bot.dbCache)
}
