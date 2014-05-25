package client.json

import client.dto.{LoginResponse, Revision, Page}
import play.api.libs.json._
import play.api.libs.functional.syntax._

object MwReads {

  //  {"login":{"result":"NeedToken","token":"a504e9507bb8e8d7d3bf839ef096f8f7","cookieprefix":"ukwiki","sessionid":"37b1d67422436e253f5554de23ae0064"}}

  def loginResponseReads: Reads[LoginResponse] = (
    (__ \ "login" \ "result").read[String] and
      (__ \ "login" \ "token").read[String]
    )(LoginResponse.apply _)

  implicit val pageReads: Reads[Page] = (
    (__ \ "pageid").read[Int] and
      (__ \ "ns").read[Int] and
      (__ \ "title").read[String]
    )(Page.noText _)

  def pagesReads(queryType: String): Reads[Seq[Page]] = (__ \ "query" \ queryType).read[Seq[Page]]

  def continueReads(continue: String): Reads[Option[String]] = (__ \ "continue" \ continue).readNullable[String]

  // for https://www.mediawiki.org/wiki/API:Legacy_Query_Continue, not supported for now
  def queryContinueReads(queryType: String, continue: String): Reads[String] = (__ \ "query-continue" \ queryType \ continue).read[String]

}

object MwReads2 {

  implicit val revisonReads: Reads[Revision] = (
    (__ \ "user").read[String] and
      (__ \ "timestamp").read[String] and
      (__ \ "comment").read[String] and
      (__ \ "*").read[String]
    )(Revision.apply _)
  val pageWithRevisionReads: Reads[Page] = (
    (__ \ "pageid").read[Int] and
      (__ \ "ns").read[Int] and
      (__ \ "title").read[String] and
      (__ \ "revisions").read[Seq[Revision]]
    )(Page.withRevisions _)

  //  def pagesWithRevisionsReads(queryType:String): Reads[Seq[Page]] = (__ \ "query" \ "pages" ).read[Seq[Page]]

}
