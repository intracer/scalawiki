package org.scalawiki.json

import org.joda.time.DateTime
import org.scalawiki.dto._
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Generator
import org.scalawiki.dto.cmd.query.list.ListArg
import org.scalawiki.dto.cmd.query.prop.PropArg
import org.scalawiki.wlx.dto.Image
import play.api.data.validation.ValidationError
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
            case "allusers" | "usercontribs" => pagesJson.asInstanceOf[JsArray].value
            case _ => pagesJson.asInstanceOf[JsArray].value
          }).map(_.asInstanceOf[JsObject])

          jsons.map {
            queryChild match {
              case "pages" => parsePage
              case "allusers" => parseUser
              case "usercontribs" => parseUserContrib
              case _ => parsePage
            }
          }.toSeq
        } else Seq.empty
      }
    }
  }

  def mwException(jsonObj: JsObject): MwException = {
    jsonObj.validate(MwReads.errorReads).get
  }

  def parsePage(pageJson: JsObject): Page = {
    val page = pageJson.validate(Parser.pageReads).get

    val revisions: Seq[Revision] = pageJson.validate(Parser.revisionsReads(page.id.get)).getOrElse(Seq.empty)
    val images: Seq[Image] = pageJson.validate(Parser.imagesReads(page.id.get, page.title)).getOrElse(Seq.empty)
    val langLinks: Map[String, String] = getLangLinks(pageJson)

    //    pageJson.validate(Parser.langLinksReads).getOrElse(Map.empty)

    page.copy(revisions = revisions, images = images, langLinks = langLinks)
  }

  // hacky wrapping into page // TODO refactor return types
  def parseUser(userJson: JsObject): Page = {
    val hasEmptyRegistration = userJson.value.get("registration").collect({case jsStr: JsString => jsStr.value.isEmpty}).getOrElse(false)
    val mappedJson = if (hasEmptyRegistration) userJson - "registration" else userJson

    val blocked = if (userJson.keys.contains("blockid")) Some(true) else None
    val user = mappedJson.validate(Parser.userReads).get.copy(blocked = blocked)
    new Page(id = None, title = user.name.get, ns = Namespace.USER_NAMESPACE, revisions = Seq(Revision(user = Some(user))))
  }

  def parseUserContrib(userJson: JsObject): Page = {
    val userContribs = userJson.validate(Parser.userContribReads).get
    userContribs.toPage
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

  def jodaDateReads(pattern: String, corrector: String => String = identity): Reads[org.joda.time.DateTime] = new Reads[org.joda.time.DateTime] {
    import org.joda.time.DateTime

    val df = org.joda.time.format.DateTimeFormat.forPattern(pattern).withZoneUTC()

    def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsNumber(d) => JsSuccess(new DateTime(d.toLong))
      case JsString(s) => parseDate(corrector(s)) match {
        case Some(d) => JsSuccess(d)
        case None => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jodadate.format", pattern))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.date"))))
    }

    private def parseDate(input: String): Option[DateTime] =
      scala.util.control.Exception.allCatch[DateTime] opt DateTime.parse(input, df)

  }

  val jodaDateTimeReads = jodaDateReads("yyyy-MM-dd'T'HH:mm:ss'Z'")

  val userReads: Reads[User] = (
    (__ \ "userid").readNullable[Long] and
      (__ \ "name").readNullable[String] and
      (__ \ "editcount").readNullable[Long] and
      (__ \ "registration").readNullable[DateTime](jodaDateTimeReads)
    )(User.apply(
    _: Option[Long],
    _: Option[String],
    _: Option[Long],
    _: Option[DateTime])
    )

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

  def imagesReads(pageId: Long, title: String): Reads[Seq[Image]] = {
    implicit val imageReads: Reads[Image] = (
      Reads.pure[String](title) and
        (__ \ "timestamp").read[DateTime](jodaDateTimeReads) and
        (__ \ "user").read[String] and
        (__ \ "size").read[Long] and
        (__ \ "width").read[Int] and
        (__ \ "height").read[Int] and
        (__ \ "url").read[String] and
        (__ \ "descriptionurl").read[String] and
        Reads.pure[Long](pageId)
      //      (__ \ "extmetadata" \ "ImageDescription" \ "value").readNullable[String] and
      //      (__ \ "extmetadata" \ "Artist" \ "value").readNullable[String]
      )(Image.basic _)

    (__ \ "imageinfo").read[Seq[Image]]
  }

  val userContribReads: Reads[UserContrib] = (
    (__ \ "userid").read[Long] and
      (__ \ "user").read[String] and
      (__ \ "pageid").read[Long] and
      (__ \ "revid").read[Long] and
      (__ \ "parentid").read[Long] and
      (__ \ "ns").read[Int] and
      (__ \ "title").read[String] and
      (__ \ "timestamp").read[DateTime](jodaDateTimeReads) and
      //      (__ \ "new").read[String] and
      //      (__ \ "minor").read[Boolea] and
      (__ \ "comment").readNullable[String] and   // can be hidden
      (__ \ "size").readNullable[Long]
    )(UserContrib.apply _)

  //  val langLinkRead: Reads[(String, String)] = (
  //    (__ \ "lang").read[String] and
  //      (__ \ "*").read[String]
  //    )(_ -> _)
  //
  //  val langLinksReads: Reads[Seq[String, String]] =
  //    (__ \ "langlinks").read[Seq[(String, String)]](langLinkRead)
}

case class UserContrib(userId: Long,
                       user: String,
                       pageId: Long,
                       revId: Long,
                       parentId: Long,
                       ns: Int,
                       title: String,
                       timestamp: DateTime,
                       //                       isNew: Boolean,
                       //                       isMinor: Boolean,
                       comment: Option[String],
                       size: Option[Long]) {

  def toPage = new Page(Some(pageId), ns, title, revisions =
    Seq(new Revision(Some(revId), Some(pageId), Some(parentId), Some(User(userId, user)), Option(timestamp), comment, None, size))
  )
}
