package org.scalawiki.json

import org.scalawiki.dto.Page.Id
import org.scalawiki.dto._
import play.api.libs.functional.syntax._
import play.api.libs.json._


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
    (__ \ "pageid").read[Id] and
      (__ \ "ns").read[Int] and
      (__ \ "title").read[String] and
      (__ \ "missing").readNullable[String]
    )(Page.noText _)

  def pagesReads(queryType: String): Reads[Seq[Page]] = (__ \ "query" \ queryType).read[Seq[Page]]

  def tokenReads: Reads[String] = (__ \ "query" \ "tokens" \ "csrftoken").read[String]

  def tokensReads: Reads[String] = (__ \ "tokens" \ "edittoken").read[String]

  def editResponseReads: Reads[String] = (__ \ "edit" \ "result").read[String]

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



