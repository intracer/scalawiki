package client.json

import client.dto.Page
import client.dto.cmd.ActionParam
import client.dto.cmd.query.Generator
import client.dto.cmd.query.list.ListArg
import client.dto.cmd.query.prop.PropArg
import play.api.libs.json._

class Parser(val action: ActionParam) {

  // val query = action.

  def parse(s: String): Seq[Page] = {
    val json = Json.parse(s)

    val queryChild = lists.headOption.fold("pages")(_.name)

    val pagesJson = (json \ "query" \ queryChild).asInstanceOf[JsObject]

   val pages = pagesJson.keys.map {
      key =>
        (pagesJson \ key).validate(Parser.pageReads).get
      }.toSeq

    pages
  }

  def query = action.query.toSeq

  def lists: Seq[ListArg] = query.flatMap(_.lists)

  def props: Seq[PropArg] = query.flatMap(_.props)

  def generator: Option[Generator] = query.flatMap(_.byType(manifest[Generator])).headOption

}

object Parser {
  import client.dto.Page.Id
  import play.api.libs.functional.syntax._

  val pageReads: Reads[Page] = (
    (__ \ "pageid").read[Id] and
      (__ \ "ns").read[Int] and
      (__ \ "title").read[String] and
      (__ \ "missing").readNullable[String] and
      (__ \ "subjectid").readNullable[Id] and
      (__ \ "talkid").readNullable[Id]
    )(Page.full _)
}
