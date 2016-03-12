package org.scalawiki.query

import org.scalawiki.dto.Page

import scala.concurrent.Future

trait SinglePageQuery {

  def whatTranscludesHere(namespaces: Set[Int] = Set.empty, continueParam: Option[(String, String)] = None): Future[Seq[Page]]

  def categoryMembers(namespaces: Set[Int] = Set.empty, continueParam: Option[(String, String)] = None): Future[Seq[Page]]

  def revisions(namespaces: Set[Int] = Set.empty, props: Set[String] = Set.empty, continueParam: Option[(String, String)] = None): Future[Seq[Page]]

  def revisionsByGenerator(
                            generator: String, generatorPrefix: String,
                            namespaces: Set[Int] = Set.empty, props: Set[String] = Set.empty,
                            continueParam: Option[(String, String)] = None,
                            limit:String = "max",
                            titlePrefix: Option[String] = None): Future[Seq[Page]]

  def imageInfoByGenerator(
                            generator: String, generatorPrefix: String,
                            namespaces: Set[Int] = Set(),
                            props: Set[String] = Set("timestamp", "user", "size", "url"/*, "extmetadata"*/),
                            continueParam: Option[(String, String)] = None,
                            limit:String = "max",
                            titlePrefix: Option[String] = None): Future[Seq[Page]]

  def edit(text: String,
           summary: Option[String] = None,
           section: Option[String] = None,
           token: Option[String] = None,
           multi:Boolean = true): Future[Any]  // TODO specific result

  def upload(filename: String): Future[Any]  // TODO specific result

}
