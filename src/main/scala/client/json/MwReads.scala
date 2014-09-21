package client.json

import client.dto._
import play.api.libs.json._
import play.api.libs.functional.syntax._

object MwReads {

  //  {"login":{"result":"NeedToken","token":"a504e9507bb8e8d7d3bf839ef096f8f7","cookieprefix":"ukwiki","sessionid":"37b1d67422436e253f5554de23ae0064"}}
  // {"login":{"result":"Success","lguserid":678,"lgusername":"IlyaBot","lgtoken":"8afaf1c733a4e667628be1f3ac176cdd","cookieprefix":"ukwiki","sessionid":"f4bf2533e14517401478383ca458feee"}}

  def loginResponseReads: Reads[LoginResponse] = (
    (__ \ "login" \ "result").read[String] and
      (__ \ "login" \ "token").readNullable[String] and
      (__ \ "login" \ "lgtoken").readNullable[String] and
      (__ \ "login" \ "lguserid").readNullable[Int] and
      (__ \ "login" \ "lgusername").readNullable[String] and
      (__ \ "login" \ "cookieprefix").readNullable[String] and
      (__ \ "login" \ "sessionid").readNullable[String]
    )(LoginResponse.apply _)

  implicit val pageReads: Reads[Page] = (
    (__ \ "pageid").read[Int] and
      (__ \ "ns").read[Int] and
      (__ \ "title").read[String]
    )(Page.noText _)

  def pagesReads(queryType: String): Reads[Seq[Page]] = (__ \ "query" \ queryType).read[Seq[Page]]

  def tokenReads: Reads[String] = (__ \ "query" \ "tokens" \ "csrftoken").read[String]
  def tokensReads: Reads[String] = (__ \ "tokens" \ "edittoken").read[String]

  def editResponseReads: Reads[String] = (__ \ "edit" \ "result" ).read[String]

  def errorReads: Reads[MwError] = (
    (__ \ "code").read[String] and
      (__ \ "info").read[String]
    )(MwError.apply _)

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
      (__ \ "height").read[Int]
//      (__ \ "url").read[String] and
//      (__ \ "descriptionurl").read[String] and
//      (__ \ "extmetadata" \ "ImageDescription" \ "value").readNullable[String] and
//      (__ \ "extmetadata" \ "Artist" \ "value").readNullable[String]
    )(ImageInfo.basic _)

  val pageWithRevisionReads: Reads[Page] = (
    (__ \ "pageid").read[Int] and
      (__ \ "ns").read[Int] and
      (__ \ "title").read[String] and
      (__ \ "edittoken").readNullable[String] and
      (__ \ "revisions").read[Seq[Revision]]
    )(Page.withRevisions _)

  val pageInfoReads: Reads[Page] = (
    (__ \ "pageid").readNullable[Int] and
      (__ \ "ns").read[Int] and
      (__ \ "title").read[String] and
      (__ \ "edittoken").readNullable[String]
    )(Page.withEditToken _)

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
