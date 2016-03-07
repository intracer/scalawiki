package org.scalawiki.json

import org.joda.time.DateTime
import org.scalawiki.dto._
import org.scalawiki.dto.cmd.{EnumArg, Action}
import org.scalawiki.dto.cmd.query.list.ListArg
import org.scalawiki.dto.cmd.query.meta.MetaArg
import org.scalawiki.dto.cmd.query.prop.PropArg
import org.scalawiki.wlx.dto.Image
import play.api.data.validation.ValidationError
import play.api.libs.json._

import scala.util.Try

class Parser(val action: Action) {

  val params = action.pairs.toMap

  var continue = Map.empty[String, String]

  def parse(str: String): Try[Seq[Page]] = {
    Try {
      val json = Json.parse(str)

      val jsonObj = json.asInstanceOf[JsObject]
      if (jsonObj.value.contains("error")) {
        throw mwException(jsonObj)
      } else {
        val queryArg = lists.headOption.orElse[EnumArg[_]](meta.headOption)
        val queryChild = queryArg.fold("pages")(arg => arg.name)

        continue = getContinue(json, jsonObj)

        if (jsonObj.value.contains("query")) {

          val pagesJson = (json \ "query" \ queryChild).get

          val jsons = (queryChild match {
            case "pages" => pagesJson.asInstanceOf[JsObject].values
            case "allusers" | "usercontribs" => pagesJson.asInstanceOf[JsArray].value
            case "globaluserinfo" => Seq(pagesJson)
            case _ => pagesJson.asInstanceOf[JsArray].value
          }).map(_.asInstanceOf[JsObject])

          jsons.map { j =>
            queryChild match {
              case "pages" => parsePage(j)
              case "allusers" | "users" => parseUser(j, queryChild)
              case "usercontribs" => parseUserContrib(j)
              case "globaluserinfo" => parseGlobalUserInfo(j)
              case _ => parsePage(j)
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

    val revisions = pageJson.validate(Parser.revisionsReads(page.id.get)).getOrElse(Seq.empty)
    val images = getImages(pageJson, page)
    val langLinks = getLangLinks(pageJson)
    val links = getLinks(pageJson)

    page.copy(revisions = revisions, images = images, langLinks = langLinks, links = links)
  }

  def getImages(pageJson: JsObject, page: Page): Seq[Image] = {
    pageJson.validate {
      if (pageJson.value.contains("imageinfo")) {
        Parser.imageInfoReads(page.id.get, page.title)
      } else {
        //      if (pageJson.value.contains("images")) {
        Parser.imageReads()
      }

    }.getOrElse(Seq.empty)
  }

  // hacky wrapping into page // TODO refactor return types
  def parseUser(userJson: JsObject, queryChild: String): Page = {
    val hasEmptyRegistration = userJson.value.get("registration").collect({ case jsStr: JsString => jsStr.value.isEmpty }).getOrElse(false)
    val mappedJson = if (hasEmptyRegistration) userJson - "registration" else userJson

    // TODO move out of loop or get from request?
    val prefix = queryChild match {
      case "allusers" => "au"
      case "users" => "us"
    }
    val props = params.get(prefix + "prop").map(_.split("\\|")).getOrElse(Array.empty[String]).toSet

    val blocked = if (props.contains("blockinfo")) Some(userJson.keys.contains("blockid")) else None
    val emailable = if (props.contains("emailable")) Some(userJson.keys.contains("emailable")) else None
    val user = mappedJson.validate(Parser.userReads).get.copy(blocked = blocked, emailable = emailable)
    new Page(id = None, title = user.name.get, ns = Namespace.USER, revisions = Seq(Revision(user = Some(user))))
  }

  def parseUserContrib(userJson: JsObject): Page = {
    val userContribs = userJson.validate(Parser.userContribReads).get
    userContribs.toPage
  }

  def getContinue(json: JsValue, jsonObj: JsObject): Map[String, String] = {
    if (jsonObj.value.contains("continue")) {
      val continueJson = (json \ "continue").get
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

  def getLinks(pageJson: JsObject): Seq[Page] = {

    def parseArray(arr: JsArray): Seq[Page] = {
      arr.value.collect {
        case link: JsObject =>
          val ns = link.value.get("ns").map { case s: JsNumber => s.value.intValue() }
          val title = link.value.get("title").map { case s: JsString => s.value }
          new Page(id = None, ns = ns.get, title = title.get)
      }
    }

    pageJson.value.get("links").toSeq.flatMap {
      case arr: JsArray => parseArray(arr)
      case _ => Seq.empty[Page]
    }
  }

  def parseGlobalUserInfo(json: JsObject) = {
    val gui = json.validate(Parser.globalUserInfoReads).get

    val user = new User(
      id = Some(gui.id),
      login = Some(gui.name),
      editCount = Some(gui.editCount),
      registration = Some(gui.registration)
    )

    new Page(id = None, title = gui.name, ns = Namespace.USER, revisions = Seq(Revision(user = Some(user))))
  }

  def query = action.query.toSeq

  def lists: Seq[ListArg] = query.flatMap(_.lists)

  def props: Seq[PropArg] = query.flatMap(_.props)

  def meta: Seq[MetaArg] = query.flatMap(_.metas)

  //  def generator: Option[Generator] = query.flatMap(_.byType(manifest[Generator])).headOption

}

object Parser {

  import play.api.libs.functional.syntax._

  val TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"
  val df = org.joda.time.format.DateTimeFormat.forPattern(TIMESTAMP_PATTERN).withZoneUTC()

  val jodaDateTimeReads = jodaDateReads(TIMESTAMP_PATTERN)

  def parseDate(input: String): DateTime = DateTime.parse(input, df)

  def parseDateOpt(input: String): Option[DateTime] =
    scala.util.control.Exception.allCatch[DateTime] opt parseDate(input)

  def jodaDateReads(pattern: String, corrector: String => String = identity): Reads[DateTime] =
    new Reads[DateTime] {
      def reads(json: JsValue): JsResult[DateTime] = json match {
        case JsNumber(d) => JsSuccess(new DateTime(d.toLong))
        case JsString(s) => parseDateOpt(corrector(s)) match {
          case Some(d) => JsSuccess(d)
          case None => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jodadate.format", pattern))))
        }
        case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.date"))))
      }
    }

  val pageReads: Reads[Page] = (
    (__ \ "pageid").read[Long] and
      (__ \ "ns").read[Int] and
      (__ \ "title").read[String] and
      (__ \ "missing").readNullable[String] and
      (__ \ "subjectid").readNullable[Long] and
      (__ \ "talkid").readNullable[Long]
    ) (Page.full _)

  val userReads: Reads[User] = (
    (__ \ "userid").readNullable[Long] and
      (__ \ "name").readNullable[String] and
      (__ \ "editcount").readNullable[Long] and
      (__ \ "registration").readNullable[DateTime](jodaDateTimeReads)
    ) (User.apply(
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
          ) (Contributor.apply _) and
        (__ \ "timestamp").readNullable[DateTime](jodaDateTimeReads) and
        (__ \ "comment").readNullable[String] and
        (__ \ "*").readNullable[String] and
        (__ \ "size").readNullable[Long] and
        (__ \ "sha1").readNullable[String] //and
      //Reads.pure[Option[Long]](None) // textId
      ) (Revision.apply(_: Option[Long],
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

  def imageInfoReads(pageId: Long, title: String): Reads[Seq[Image]] = {
    implicit val imageReads: Reads[Image] = (
      Reads.pure[String](title) and
        (__ \ "timestamp").readNullable[DateTime](jodaDateTimeReads) and
        (__ \ "user").readNullable[String] and
        (__ \ "size").readNullable[Long] and
        (__ \ "width").readNullable[Int] and
        (__ \ "height").readNullable[Int] and
        (__ \ "url").readNullable[String] and
        (__ \ "descriptionurl").readNullable[String] and
        Reads.pure[Option[Long]](Some(pageId))
      //      (__ \ "extmetadata" \ "ImageDescription" \ "value").readNullable[String] and
      //      (__ \ "extmetadata" \ "Artist" \ "value").readNullable[String]
      ) (Image.basic _)

    (__ \ "imageinfo").read[Seq[Image]]
  }

  def imageReads(): Reads[Seq[Image]] = {
    implicit val imageReads: Reads[Image] = (
      (__ \ "title").read[String] and
        (__ \ "timestamp").readNullable[DateTime](jodaDateTimeReads) and
        (__ \ "user").readNullable[String] and
        (__ \ "size").readNullable[Long] and
        (__ \ "width").readNullable[Int] and
        (__ \ "height").readNullable[Int] and
        (__ \ "url").readNullable[String] and
        (__ \ "descriptionurl").readNullable[String] and
        Reads.pure[Option[Long]](None)
      //      (__ \ "extmetadata" \ "ImageDescription" \ "value").readNullable[String] and
      //      (__ \ "extmetadata" \ "Artist" \ "value").readNullable[String]
      ) (Image.basic _)

    (__ \ "images").read[Seq[Image]]
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
      (__ \ "comment").readNullable[String] and // can be hidden
      (__ \ "size").readNullable[Long]
    ) (UserContrib.apply _)

  def globalUserInfoReads: Reads[GlobalUserInfo] = {

    implicit val sulAccountReads: Reads[SulAccount] = (
      (__ \ "wiki").read[String] and
        (__ \ "url").read[String] and
        (__ \ "timestamp").read[DateTime](jodaDateTimeReads) and
        (__ \ "method").read[String] and
        (__ \ "editcount").read[Long] and
        (__ \ "registration").read[DateTime](jodaDateTimeReads)
      ) (SulAccount.apply _)

    (
      (__ \ "home").read[String] and
        (__ \ "id").read[Long] and
        (__ \ "registration").read[DateTime](jodaDateTimeReads) and
        (__ \ "name").read[String] and
        (__ \ "merged").read[Seq[SulAccount]] and
        (__ \ "editcount").read[Long]
      ) (GlobalUserInfo.apply _)
  }

  //  val langLinkRead: Reads[(String, String)] = (
  //    (__ \ "lang").read[String] and
  //      (__ \ "*").read[String]
  //    )(_ -> _)
  //
  //  val langLinksReads: Reads[Seq[String, String]] =
  //    (__ \ "langlinks").read[Seq[(String, String)]](langLinkRead)
}