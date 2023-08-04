package org.scalawiki.query

import org.scalawiki.MwBot
import org.scalawiki.dto.Page

import scala.concurrent.Future

trait PageQuery {

  def revisions(
      namespaces: Set[Int] = Set.empty,
      props: Set[String] = Set.empty,
      continueParam: Option[(String, String)] = None): Future[Iterable[Page]]

}

object PageQuery {

  def byTitles(titles: Set[String], bot: MwBot): PageQuery =
    new PageQueryImplDsl(Right(titles), bot)

  def byTitle(title: String, bot: MwBot): SinglePageQuery =
    new PageQueryImplDsl(Right(Set(title)), bot)

  def byIds(ids: Set[Long], bot: MwBot): PageQuery =
    new PageQueryImplDsl(Left(ids), bot)

  def byId(id: Long, bot: MwBot): SinglePageQuery =
    new PageQueryImplDsl(Left(Set(id)), bot)
}
