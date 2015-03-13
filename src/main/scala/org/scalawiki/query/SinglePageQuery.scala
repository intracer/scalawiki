package org.scalawiki.query

import org.scalawiki.dto.Page

import scala.concurrent.Future

trait SinglePageQuery {

  def whatTranscludesHere(namespaces: Set[Int] = Set.empty, continueParam: Option[(String, String)] = None): Future[Seq[Page]]

  def categoryMembers(namespaces: Set[Int] = Set.empty, continueParam: Option[(String, String)] = None): Future[Seq[Page]]

  def edit(text: String, summary: String, token: Option[String] = None, multi:Boolean = true): Future[Any]  // TODO specific result

  def upload(filename: String): Future[Any]  // TODO specific result

}
