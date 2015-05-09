package org.scalawiki.json

import org.joda.time.DateTime
import org.scalawiki.dto._
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Generator
import org.scalawiki.dto.cmd.query.list.ListArg
import org.scalawiki.dto.cmd.query.prop.PropArg
import play.api.libs.json._

import scala.util.Try

class Parser(val action: Action) {

  var continue = Map.empty[String, String]

  def parse(str: String): Try[Seq[Page]] = {
    Try {
      val json = Json.parse(str)

      val jsonObj = json.asInstanceOf[JsObject]
      if (jsonObj.value.contains("error")) {
        throw mwException(jsonObj)
      } else {
        val queryChild = lists.headOption.fold("pages")(_.name)

        continue = getContinue(json, jsonObj)

        if (jsonObj.value.contains("query")) {

          val pagesJson = json \ "query" \ queryChild

          val jsons = (queryChild match {
            case "pages" => pagesJson.asInstanceOf[JsObject].values
            case _ => pagesJson.asInstanceOf[JsArray].value
          }).map(_.asInstanceOf[JsObject])

          jsons.map(parsePage).toSeq
        } else Seq.empty
      }
    }
  }

  def mwException(jsonObj: JsObject): MwException = {
    jsonObj.validate(MwReads.errorReads).get
  }

  def getLangLinks(pageJson: JsObject): Map[String, String] = {

    def parseArray(arr: JsArray): Seq[(String, String)] = {
      arr.value.collect {
        case ll: JsObject =>
          val lang = ll.value.get("lang").map { case s: JsString => s.value }
          val page = ll.value.get("*").map { case s: JsString => s.value }
          lang.get -> page.get
      }
    }

    pageJson.value.get("langlinks").toSeq.flatMap {
      case arr: JsArray => parseArray(arr)
      case _ => Seq.empty[(String, String)]
    }.toMap

  }

  def parsePage(pageJson: JsObject): Page = {
    val page = pageJson.validate(Parser.pageReads).get

    val revisions: Seq[Revision] = pageJson.validate(Parser.revisionsReads(page.id.get)).getOrElse(Seq.empty)
    val imageInfos: Seq[ImageInfo] = pageJson.validate(Parser.imageInfosReads).getOrElse(Seq.empty)
    val langLinks: Map[String, String] = getLangLinks(pageJson)

    //    pageJson.validate(Parser.langLinksReads).getOrElse(Map.empty)

    page.copy(revisions = revisions, imageInfo = imageInfos, langLinks = langLinks)
  }

  def getContinue(json: JsValue, jsonObj: JsObject): Map[String, String] = {
    if (jsonObj.value.contains("continue")) {
      val continueJson = json \ "continue"
      continueJson match {
        case obj: JsObject => obj.fields.toMap.mapValues[String] {
          case JsNumber(n) => n.toString()
          case JsString(s) => s
          case _ => throw new scala.IllegalArgumentException(obj.toString())
        }
        case _ => Map.empty[String, String]
      }
    } else Map.empty[String, String]
  }

  def query = action.query.toSeq

  def lists: Seq[ListArg] = query.flatMap(_.lists)

  def props: Seq[PropArg] = query.flatMap(_.props)

  def generator: Option[Generator] = query.flatMap(_.byType(manifest[Generator])).headOption

}


object Parser {

  import play.api.libs.functional.syntax._

  val pageReads: Reads[Page] = (
    (__ \ "pageid").read[Long] and
      (__ \ "ns").read[Int] and
      (__ \ "title").read[String] and
      (__ \ "missing").readNullable[String] and
      (__ \ "subjectid").readNullable[Long] and
      (__ \ "talkid").readNullable[Long]
    )(Page.full _)

  // implicit val DefaultJodaDateReads = jodaDateReads()

  val jodaDateTimeReads = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss'Z'")


  def revisionsReads(pageId: Long): Reads[Seq[Revision]] = {
    implicit val revisionReads: Reads[Revision] = (
      (__ \ "revid").readNullable[Long] and
        Reads.pure[Option[Long]](Some(pageId)) and
        (__ \ "parentid").readNullable[Long] and
        (
          (__ \ "userid").readNullable[Long] and
            (__ \ "user").readNullable[String]
          )(Contributor.apply _) and
        (__ \ "timestamp").readNullable[DateTime](jodaDateTimeReads) and
        (__ \ "comment").readNullable[String] and
        (__ \ "*").readNullable[String] and
        (__ \ "size").readNullable[Long] and
        (__ \ "sha1").readNullable[String] //and
      //Reads.pure[Option[Long]](None) // textId
      )(Revision.apply(_: Option[Long],
      _: Option[Long],
      _: Option[Long],
      _: Option[Contributor],
      _: Option[DateTime],
      _: Option[String],
      _: Option[String],
      _: Option[Long],
      _: Option[String]))

    (__ \ "revisions").read[Seq[Revision]]
  }

  implicit val imageInfoReads: Reads[ImageInfo] = (
    (__ \ "timestamp").read[String] and
      (__ \ "user").read[String] and
      (__ \ "size").read[Long] and
      (__ \ "width").read[Int] and
      (__ \ "height").read[Int] and
      (__ \ "url").read[String] and
      (__ \ "descriptionurl").read[String]
    //      (__ \ "extmetadata" \ "ImageDescription" \ "value").readNullable[String] and
    //      (__ \ "extmetadata" \ "Artist" \ "value").readNullable[String]
    )(ImageInfo.basic _)

  val imageInfosReads: Reads[Seq[ImageInfo]] = (__ \ "imageinfo").read[Seq[ImageInfo]]

  //  val langLinkRead: Reads[(String, String)] = (
  //    (__ \ "lang").read[String] and
  //      (__ \ "*").read[String]
  //    )(_ -> _)
  //
  //  val langLinksReads: Reads[Seq[String, String]] =
  //    (__ \ "langlinks").read[Seq[(String, String)]](langLinkRead)

}
