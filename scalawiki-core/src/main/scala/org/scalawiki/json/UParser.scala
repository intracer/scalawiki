package org.scalawiki.json

import org.scalawiki.dto._
import org.scalawiki.dto.cmd.query.list.ListArg
import org.scalawiki.dto.cmd.query.meta.MetaArg
import org.scalawiki.dto.cmd.{Action, EnumArg}
import org.scalawiki.json.UParser.pageMain
import org.scalawiki.json.playjson.{CategoryInfoReads, GlobalUserInfoReads, ImageReads, PlayParser, UserContributorReads, UserReads, WikiResponseReads}
import play.api.libs.json.{JsObject, Reads, __}
import ujson.Value
import upickle.default
import upickle.default._

import scala.util.Try

class UParser(val action: Action) extends Parser with PageReader {

  private val params = action.pairs.toMap

  var continue = Map.empty[String, String]

  implicit val mwExceptionR: default.Reader[MwException] = macroR[MwException]

  override def parse(str: String): Try[Seq[Page]] = {
    Try {
      val json = ujson.read(str)

      val obj = json.obj
      if (obj.contains("error")) {
        throw mwException(json("error"))
      }

      val queryArg = lists.headOption.orElse[EnumArg[_]](meta.headOption)
      val queryChild = queryArg.fold("pages")(arg => arg.name)

      continue = getContinue(json)
      obj.get("query").map(parseQueryAction(_, queryChild)).getOrElse(Nil)
    }
  }

  private def parseQueryAction(json: Value, queryChild: String): Seq[Page] = {
    val pagesJson = json(queryChild)

    val jsons = queryChild match {
      case "pages"                     => pagesJson.obj.values
      case "allusers" | "usercontribs" => pagesJson.arr
      case "globaluserinfo"            => Seq(pagesJson)
      case _                           => pagesJson.arr
    }

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

  private def mwException(json: Value): MwException = {
    upickle.default.read[MwException](json)
  }

  def parsePage(pageJson: Value): Page = {
    val page = read[Page](pageJson)

    val revisions = page.id.map { pageId =>
      UParser.revisions(pageId, pageJson).toSeq
    }.getOrElse(Nil)

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

  def getImages(pageJson: Value, page: Page): Seq[Image] = {
        if (pageJson.obj.contains("imageinfo")) {
          UParser.imageInfoReads(page.id, Some(page.title))
        } else {
          UParser.imageReads()
        }
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

  private def getContinue(json: Value): Map[String, String] = {
    json.obj.get("continue").obj.view.mapValues(_.str).toMap
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

  private def query = action.query.toSeq

  private def lists: Seq[ListArg] = query.flatMap(_.lists)

  private def meta: Seq[MetaArg] = query.flatMap(_.metas)

}

object UParser extends WikiResponseReads {

  def pageMain(json: Value) = {
    val obj = json.obj
    Page.full(
      id = obj.get("pageid").map(_.num.toLong),
      ns = obj.get("ns").map(_.num.toInt),
      title = obj.get("title").str,
      missing = obj.contains("missing"),
      subjectId = obj.get("subjectid").map(_.num.toLong),
      talkId = obj.get("talkid").map(_.num.toLong),
      invalidReason = obj.get("invalidreason").map(_.str)
    )
  }

  private val userReads: Reads[User] = UserReads()

  private def revisions(pageId: Long, pageJson: Value) = {
    pageJson.obj.get("revisions").arr.map { json =>
      val obj = json.obj
      Revision(
        revId = obj.get("revid").map(_.num.toLong),
        pageId = Some(pageId),
        parentId = obj.get("parentid").map(_.num.toLong),
        Contributor.apply(
          obj.get("userid").map(_.num.toLong),
          obj.get("user").map(_.str)
        ),
        timestamp = obj.get("timestamp").map(_.str).flatMap(parseDateOpt),
        comment = obj.get("comment").map(_.str),
        content = obj.get("*").map(_.str),
        size = obj.get("size").map(_.num.toLong),
        sha1 = obj.get("sha1").map(_.str)
      )
    }
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
