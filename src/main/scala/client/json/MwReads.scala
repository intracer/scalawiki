package client.json

import client.dto._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import client.dto.Revision
import client.dto.Continue
import client.dto.LoginResponse

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

  def continueReads(continue: String): Reads[Continue] = (
    (__ \ "continue" \ "continue").readNullable[String] and
      (__ \ "continue" \ continue).readNullable[String]
    )(Continue.apply _)


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

  implicit val imageInfoReads: Reads[ImageInfo] = (
    (__ \ "timestamp").read[String] and
      (__ \ "user").read[String] and
      (__ \ "size").read[Int] and
      (__ \ "width").read[Int] and
      (__ \ "height").read[Int] and
      (__ \ "url").read[String] and
      (__ \ "descriptionurl").read[String]
    )(ImageInfo.apply _)

  val pageWithRevisionReads: Reads[Page] = (
    (__ \ "pageid").read[Int] and
      (__ \ "ns").read[Int] and
      (__ \ "title").read[String] and
      (__ \ "revisions").read[Seq[Revision]]
    )(Page.withRevisions _)

  val pageWithImageInfoReads: Reads[Page] = (
    (__ \ "pageid").read[Int] and
      (__ \ "ns").read[Int] and
      (__ \ "title").read[String] and
      (__ \ "imageinfo").read[Seq[ImageInfo]]
    )(Page.withImageInfo _)

  //  def pagesWithRevisionsReads(queryType:String): Reads[Seq[Page]] = (__ \ "query" \ "pages" ).read[Seq[Page]]

}

//"timestamp": "2014-05-20T20:54:33Z",
//      "user": "Taras r",
//      "size": 4270655,
//      "width": 3648,
//      "height": 2736,
//      "url": "https://upload.wikimedia.org/wikipedia/commons/e/ea/%22Dovbush-rocks%22_01.JPG",
//      "descriptionurl": "https://commons.wikimedia.org/wiki/File:%22Dovbush-rocks%22_01.JPG"
