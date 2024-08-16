package org.scalawiki.json.playjson

import org.scalawiki.dto._
import org.scalawiki.dto.cmd.query.list.ListArg
import org.scalawiki.dto.cmd.query.meta.MetaArg
import org.scalawiki.dto.cmd.query.prop.PropArg
import org.scalawiki.dto.cmd.{Action, EnumArg}
import org.scalawiki.json.Parser
import play.api.libs.json._

import scala.util.Try

class PlayParser(val action: Action) extends Parser {

  private val params = action.pairs.toMap

  var continue = Map.empty[String, String]

  override def parse(str: String): Try[Seq[Page]] = {
    Try {
      val json = Json.parse(str)
      val jsonObj = json.asInstanceOf[JsObject]

      if (jsonObj.value.contains("error")) {
        throw mwException(jsonObj)
      }

      val queryArg = lists.headOption.orElse[EnumArg[_]](meta.headOption)
      val queryChild = queryArg.fold("pages")(arg => arg.name)

      continue = getContinue(json)
      jsonObj.value match {
        case value if value.contains("query") =>
          parseQueryAction(json, queryChild)
        case _ => Seq.empty
      }
    }
  }

  def parseQueryAction(json: JsValue, queryChild: String): Seq[Page] = {
    val pagesJson = (json \ "query" \ queryChild).get

    val jsons = (queryChild match {
      case "pages"                     => pagesJson.asInstanceOf[JsObject].values
      case "allusers" | "usercontribs" => pagesJson.asInstanceOf[JsArray].value
      case "globaluserinfo"            => Seq(pagesJson)
      case _                           => pagesJson.asInstanceOf[JsArray].value
    }).map(_.asInstanceOf[JsObject])

    jsons.map { j =>
      queryChild match {
        case "pages"              => parsePage(j)
        case "allusers" | "users" => parseUser(j, queryChild)
        case "usercontribs"       => parseUserContrib(j)
        case "globaluserinfo"     => parseGlobalUserInfo(j)
        case _                    => parsePage(j)
      }
    }.toSeq
  }

  def mwException(jsonObj: JsObject): MwException = {
    jsonObj.validate(MwReads.errorReads).get
  }

  def parsePage(pageJson: JsObject): Page = {
    val page = pageJson.validate(PlayParser.pageReads).get

    val revisions = page.id.fold(Seq.empty[Revision]) { pageId =>
      pageJson.validate(PlayParser.revisionsReads(pageId)).getOrElse(Seq.empty)
    }

    val images = getImages(pageJson, page)
    val langLinks = getLangLinks(pageJson)
    val links = getLinks(pageJson)
    val categoryInfo = getCategoryInfo(pageJson)

    page.copy(
      revisions = revisions,
      images = images,
      langLinks = langLinks,
      links = links,
      categoryInfo = categoryInfo
    )
  }

  def getImages(pageJson: JsObject, page: Page): Seq[Image] = {
    pageJson
      .validate {
        if (pageJson.value.contains("imageinfo")) {
          PlayParser.imageInfoReads(page.id, Some(page.title))
        } else {
          //      if (pageJson.value.contains("images")) {
          PlayParser.imageReads()
        }
      }
      .getOrElse(Seq.empty)
  }

  // hacky wrapping into page // TODO refactor return types
  def parseUser(userJson: JsObject, queryChild: String): Page = {
    val hasEmptyRegistration = userJson.value
      .get("registration")
      .collect({ case jsStr: JsString => jsStr.value.isEmpty })
      .getOrElse(false)
    val mappedJson =
      if (hasEmptyRegistration) userJson - "registration" else userJson

    // TODO move out of loop or get from request?
    val prefix = queryChild match {
      case "allusers" => "au"
      case "users"    => "us"
    }
    val props = params
      .get(prefix + "prop")
      .map(_.split("\\|"))
      .getOrElse(Array.empty[String])
      .toSet

    val blocked =
      if (props.contains("blockinfo")) Some(userJson.keys.contains("blockid"))
      else None
    val emailable =
      if (props.contains("emailable")) Some(userJson.keys.contains("emailable"))
      else None
    val jsResult = mappedJson.validate(PlayParser.userReads)
    val user = jsResult.get.copy(blocked = blocked, emailable = emailable)
    new Page(
      id = None,
      title = user.name.get,
      ns = Some(Namespace.USER),
      revisions = Seq(Revision(user = Some(user)))
    )
  }

  def parseUserContrib(userJson: JsObject): Page = {
    val userContribs = userJson.validate(PlayParser.userContribReads).get
    userContribs.toPage
  }

  def getContinue(json: JsValue): Map[String, String] = {
    (json \ "continue")
      .asOpt[JsObject]
      .map(
        _.value
          .mapValues[String] {
            case JsNumber(n) => n.toString()
            case JsString(s) => s
          }
          .toMap
      )
      .getOrElse(Map.empty[String, String])
  }

  def getLangLinks(pageJson: JsObject): Map[String, String] = {
    (pageJson \ "langlinks")
      .asOpt[Seq[Map[String, String]]]
      .map {
        _.map(l => l("lang") -> l("*")).toMap
      }
      .getOrElse(Map.empty[String, String])
  }

  def getLinks(pageJson: JsObject): Seq[Page] = {
    (pageJson \ "links")
      .asOpt[JsArray]
      .map {
        _.value
          .map { l =>
            new Page(
              id = None,
              ns = (l \ "ns").asOpt[Int],
              title = (l \ "title").as[String]
            )
          }
          .toSeq
      }
      .getOrElse(Nil)
  }

  def getCategoryInfo(pageJson: JsObject): Option[CategoryInfo] =
    pageJson.validate(PlayParser.categoryInfoReads()).getOrElse(None)

  def parseGlobalUserInfo(json: JsObject) = {
    if (!json.value.contains("missing")) {

      val gui = json.validate(PlayParser.globalUserInfoReads).get

      val user = new User(
        id = Some(gui.id),
        login = Some(gui.name),
        editCount = Some(gui.editCount),
        registration = Some(gui.registration),
        sulAccounts = gui.merged
      )

      new Page(
        id = None,
        title = gui.name,
        ns = Some(Namespace.USER),
        revisions = Seq(Revision(user = Some(user)))
      )
    } else {
      new Page(
        id = None,
        title = "missing",
        ns = Some(Namespace.USER),
        revisions = Seq.empty
      )
    }
  }

  def query = action.query.toSeq

  def lists: Seq[ListArg] = query.flatMap(_.lists)

  def props: Seq[PropArg] = query.flatMap(_.props)

  def meta: Seq[MetaArg] = query.flatMap(_.metas)

  //  def generator: Option[Generator] = query.flatMap(_.byType(manifest[Generator])).headOption

}

object PlayParser {

  private val pageReads: Reads[Page] = PageReads()

  private val userReads: Reads[User] = UserReads()

  private def revisionsReads(pageId: Long): Reads[Seq[Revision]] = {
    implicit val revisionReads: Reads[Revision] = RevisionRead(Some(pageId))
    (__ \ "revisions").read[Seq[Revision]]
  }

  private def imageInfoReads(
      pageId: Option[Long],
      title: Option[String]
  ): Reads[Seq[Image]] = {
    implicit val imageReads: Reads[Image] =
      ImageReads(title = title, pageId = pageId)
    (__ \ "imageinfo").read[Seq[Image]]
  }

  private def imageReads(): Reads[Seq[Image]] = {
    implicit val imageReads: Reads[Image] = ImageReads(None, None)
    (__ \ "images").read[Seq[Image]]
  }

  private def categoryInfoReads(): Reads[Option[CategoryInfo]] = {
    implicit val categoryInfoReads: Reads[CategoryInfo] = CategoryInfoReads()
    (__ \ "categoryinfo").readNullable[CategoryInfo]
  }

  private def globalUserInfoReads: Reads[GlobalUserInfo] = GlobalUserInfoReads()

  private def userContribReads: Reads[UserContrib] = UserContributorReads()
}
