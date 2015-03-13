package org.scalawiki.query

import org.scalawiki.MwBot
import org.scalawiki.dto.Page

import scala.concurrent.Future

trait PageQuery {

  def revisions(namespaces: Set[Int] = Set.empty, props: Set[String] = Set.empty, continueParam: Option[(String, String)] = None): Future[Seq[Page]]

  def imageInfoByGenerator(
                            generator: String, generatorPrefix: String,
                            namespaces: Set[Int] = Set(),
                            props: Set[String] = Set("timestamp", "user", "size", "url"/*, "extmetadata"*/),
                            continueParam: Option[(String, String)] = None,
                            limit:String = "max",
                            titlePrefix: Option[String] = None): Future[Seq[Page]]

    def revisionsByGenerator(
                              generator: String, generatorPrefix: String,
                              namespaces: Set[Int] = Set.empty, props: Set[String] = Set.empty,
                              continueParam: Option[(String, String)] = None,
                              limit:String = "max",
                              titlePrefix: Option[String] = None): Future[Seq[Page]]


}


object PageQuery {
  def byTitles(titles: Set[String], site: MwBot) = new PageQueryImplV1(Right(titles), site)

  def byTitle(title: String, site: MwBot) = new SinglePageQueryImplV1(Right(title), site)

  def byIds(ids: Set[Long], site: MwBot) = new PageQueryImplV1(Left(ids), site)

  def byId(id: Long, site: MwBot) = new SinglePageQueryImplV1(Left(id), site)
}
