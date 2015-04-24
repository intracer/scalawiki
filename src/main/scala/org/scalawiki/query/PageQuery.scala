package org.scalawiki.query

import org.scalawiki.MwBot
import org.scalawiki.dto.Page

import scala.concurrent.Future

trait PageQuery {

  def revisions(namespaces: Set[Int] = Set.empty, props: Set[String] = Set.empty, continueParam: Option[(String, String)] = None): Future[Seq[Page]]

}


object PageQuery {
  def byTitles(titles: Set[String], site: MwBot): PageQuery = new PageQueryImplDsl(Right(titles), site)

  def byTitle(title: String, site: MwBot): SinglePageQuery = new PageQueryImplDsl(Right(Set(title)), site)

  def byIds(ids: Set[Long], site: MwBot):PageQuery = new PageQueryImplDsl(Left(ids), site)

  def byId(id: Long, site: MwBot): SinglePageQuery = new PageQueryImplDsl(Left(Set(id)), site)
}
