package org.scalawiki.json

import org.joda.time.DateTime
import org.scalawiki.MwException
import org.scalawiki.dto._
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Generator
import org.scalawiki.dto.cmd.query.list.ListArg
import org.scalawiki.dto.cmd.query.prop.PropArg
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

class Parser(val action: Action) {

  var continue = Map.empty[String, String]

  def parse(s: String): Try[Seq[Page]] = {
    val json = Json.parse(s)

    val jsonObj = json.asInstanceOf[JsObject]
    if (jsonObj.value.contains("error")){
      val error = jsonObj.value("error").asInstanceOf[JsObject]
      val code = error.value("code").asInstanceOf[JsString].value
      val info = error.value("info").asInstanceOf[JsString].value
      Failure(MwException(code, info))
    } else {

      val queryChild = lists.headOption.fold("pages")(_.name)

      val pagesJson = json \ "query" \ queryChild

      val jsons = (queryChild match {
        case "pages" => pagesJson.asInstanceOf[JsObject].values
        case _ => pagesJson.asInstanceOf[JsArray].value
      }).map(_.asInstanceOf[JsObject])

      continue = json \ "continue" match {
        case obj: JsObject => obj.fields.toMap.mapValues(_.as[String])
        case _ => Map.empty[String, String]
      }

      Success(jsons.map(parsePage).toSeq)
    }
  }

  def parsePage(pageJson: JsObject): Page = {
    val page = pageJson.validate(Parser.pageReads).get

    val revisions: Seq[Revision] = pageJson.validate(Parser.revisionsReads(page.id.get)).getOrElse(Seq.empty)
    val imageInfos: Seq[ImageInfo] = pageJson.validate(Parser.imageInfosReads).getOrElse(Seq.empty)

    page.copy(revisions = revisions, imageInfo = imageInfos)
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

}
