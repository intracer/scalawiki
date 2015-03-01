package org.scalawiki.json

import org.scalawiki.dto.{Revision, Page}
import org.scalawiki.dto.cmd.ActionParam
import org.scalawiki.dto.cmd.query.Generator
import org.scalawiki.dto.cmd.query.list.ListArg
import org.scalawiki.dto.cmd.query.prop.PropArg
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{LocalDateTime, DateTime}
import play.api.libs.json._

class Parser(val action: ActionParam) {

  // val query = action.

  def parse(s: String): Seq[Page] = {
    val json = Json.parse(s)

    val queryChild = lists.headOption.fold("pages")(_.name)

    val pagesJson = (json \ "query" \ queryChild).asInstanceOf[JsObject]

    pagesJson.keys.map {
      key =>
        val pageJson = (pagesJson \ key).asInstanceOf[JsObject]
        var page = pageJson.validate(Parser.pageReads).get

        if (pageJson.keys.contains("revisions")) {
          val revisions:Seq[Revision] = pageJson.validate(Parser.revisionsReads).get

          page =
            try {
              page.copy(revisions = revisions)
            } catch {
              case e: Throwable =>
                println(e)
                throw  e
            }
        }


        page
    }.toSeq

  }

  def query = action.query.toSeq

  def lists: Seq[ListArg] = query.flatMap(_.lists)

  def props: Seq[PropArg] = query.flatMap(_.props)

  def generator: Option[Generator] = query.flatMap(_.byType(manifest[Generator])).headOption

}

object Parser {

  import org.scalawiki.dto.Page.Id

  import play.api.libs.functional.syntax._

  val pageReads: Reads[Page] = (
    (__ \ "pageid").read[Id] and
      (__ \ "ns").read[Int] and
      (__ \ "title").read[String] and
      (__ \ "missing").readNullable[String] and
      (__ \ "subjectid").readNullable[Id] and
      (__ \ "talkid").readNullable[Id]
    )(Page.full _)

 // implicit val DefaultJodaDateReads = jodaDateReads()

  val jodaDateTimeReads = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss'Z'")

  implicit val revisonReads: Reads[Revision] = (
    (__ \ "revid").readNullable[Id] and
      (__ \ "parentid").readNullable[Id] and
      (__ \ "user").readNullable[String] and
      (__ \ "userid").readNullable[Id] and
      (__ \ "timestamp").readNullable[DateTime](jodaDateTimeReads) and
      (__ \ "comment").readNullable[String] and
      (__ \ "*").readNullable[String] and
      (__ \ "size").readNullable[Int] and
      (__ \ "sha1").readNullable[String]
    )(Revision.apply _)

  val revisionsReads: Reads[Seq[Revision]] = (__ \ "revisions").read[Seq[Revision]]
}
