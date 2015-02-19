package client.dto

import java.nio.file.{Files, Paths}

import client.json.MwReads._
import client.json.MwReads2
import client.{MwBot, MwUtils}
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.Future

class PageQuery(query: Either[Set[Long], Set[String]], site: MwBot) {

  def toMap(paramNames:(String, String)): Map[String, String] = {
    val paramValues:Set[String] = query.fold(_.map(_.toString), identity)
    val paramName = query.fold(_ => paramNames._1, _ => paramNames._2)
    Map(paramName -> paramValues.map(MwUtils.normalize).mkString("|"))
  }

  def revisions(namespaces: Set[Int] = Set.empty, props: Set[String] = Set.empty, continueParam: Option[(String, String)] = None) = {
    queryProps(namespaces, props, continueParam, "revisions", "rv") /*Set("content")*/
  }

  def imageInfoByGenerator(
                            generator: String, generatorPrefix: String,
                            namespaces: Set[Int] = Set(),
                            props: Set[String] = Set("timestamp", "user", "size", "url"/*, "extmetadata"*/),
                            continueParam: Option[(String, String)] = None,
                            limit:String = "max",
                            titlePrefix: Option[String] = None) = {
    queryByGenerator(generator, generatorPrefix, namespaces, props, continueParam, "imageinfo", "ii", limit, titlePrefix)
  }

  def revisionsByGenerator(
                            generator: String, generatorPrefix: String,
                            namespaces: Set[Int] = Set.empty, props: Set[String] = Set.empty,
                            continueParam: Option[(String, String)] = None,
                            limit:String = "max",
                            titlePrefix: Option[String] = None) = {
    queryByGenerator(generator, generatorPrefix, namespaces, props, continueParam, "revisions", "rv", limit, titlePrefix)
  }

  def editToken = {
    queryProps(queryType = "info", queryPrefix = "", extraParams = Map("intoken" -> "edit"))
  }

  def queryList(namespaces: Set[Int] = Set.empty, continueParam: Option[(String, String)], queryType: String, queryPrefix: String) = {
    query(namespaces, continueParam, "list", queryType, queryPrefix)
  }

  def queryProps(
                  namespaces: Set[Int] = Set.empty,
                  props: Set[String] = Set.empty,
                  continueParam: Option[(String, String)] = None,
                  queryType: String,
                  queryPrefix: String,
                  limit:String = "max",
                  extraParams: Map[String, String] = Map.empty
                  ) = {
    val propsParams: Map[String, String] = if (props.isEmpty) Map.empty else Map(queryPrefix + "prop" -> props.mkString("|"))
    query(namespaces, continueParam, "prop", queryType, queryPrefix, limit, extraParams ++ propsParams)
  }

  def queryByGenerator(generator: String, generatorPrefix: String,
                       namespaces: Set[Int] = Set.empty,
                       props: Set[String] = Set.empty,
                       continueParam: Option[(String, String)],
                       queryType: String,
                       queryPrefix: String,
                       limit:String = "max",
                       titlePrefix: Option[String] = None) = {
    val extraParams: Map[String, String] = if (props.isEmpty) Map.empty else Map(queryPrefix + "prop" -> props.mkString("|"))
    query(namespaces, continueParam, "prop", queryType, queryPrefix, limit, extraParams, Some(generator), Option(generatorPrefix), titlePrefix = titlePrefix)
  }

  def query(
             namespaces: Set[Int] = Set.empty,
             continueParam: Option[(String, String)],
             module: String,
             queryType: String,
             queryPrefix: String,
             limit:String = "max",
             extraParams: Map[String, String] = Map.empty,
             generator: Option[String] = None,
             generatorPrefix: Option[String] = None,
             previousPages: Seq[Page] = Seq.empty,
             titlePrefix: Option[String] = None): Future[Seq[Page]] = {
    val params = makeParams(namespaces, continueParam, module, queryType, queryPrefix, limit, extraParams, generator, generatorPrefix, titlePrefix)

    import site.system.dispatcher
    import scala.concurrent._

    site.get(params) flatMap {
      body =>
        val json = Json.parse(body)

        val pages: Seq[Page] = module match {
          case "list" => json.validate(pagesReads(queryType)).getOrElse(Seq.empty)
          case "prop" =>
            val pagesJson = (json \ "query" \ "pages").asInstanceOf[JsObject]
            pagesJson.keys.map {
              key =>
                val pageJson: JsValue = pagesJson \ key
                val reads = queryType match {
                  case "revisions" => MwReads2.pageWithRevisionReads
                  case "info" => MwReads2.pageInfoReads
                  case "imageinfo" => MwReads2.pageWithImageInfoReads
                }
                val page = pageJson.validate(reads).get
                page
            }.toSeq
          case _ => Seq.empty
        }


        val continueParamName = generatorPrefix.fold(queryPrefix)(s => "g" + s) + "continue"
        val continue = json.validate(continueReads(continueParamName)).asOpt

        site.system.log.info(s"pages: ${pages.size}, $continueParamName: $continue")

        continue.fold( future {
          val result = previousPages ++ pages
          site.system.log.info(s"""query: ${toMap("id", "title")} , size: ${result.size}""")
          result
        }) { c =>
          query(namespaces, Some(c.continue.get, c.prefixed.get), module, queryType, queryPrefix, limit, extraParams, generator, generatorPrefix, previousPages ++ pages)
        }
    }
  }

  def makeParams(namespaces: Set[Int],
                  continueParam: Option[(String, String)],
                  module: String,
                  queryType: String,
                  queryPrefix: String,
                  limit: String = "max",
                  extraParams: Map[String, String] = Map.empty,
                  generator: Option[String] = None,
                  generatorPrefix: Option[String] = None,
                  titlePrefix: Option[String] = None): Map[String, String] = {
    val querySuffix = module match {
      case "list" => ""
      case "prop" => generator.fold("s")(s => titlePrefix.fold("")(_ => "s"))
    }
    val queryPrefixWithGen = generatorPrefix.fold(queryPrefix)(s => "g" + s)

    val actualTitlePrefix = titlePrefix.orElse(generatorPrefix.map("g" + _))

    val queryParamNames = module match {
      case "list" => (queryPrefixWithGen + "pageid" + querySuffix, queryPrefixWithGen + "title" + querySuffix)
      case "prop" => (
        actualTitlePrefix.fold("pageid" + querySuffix)( _ + "pageid" + querySuffix),
        actualTitlePrefix.fold("title" + querySuffix)( _ + "title" + querySuffix))
    }

    val limits = module match {
      case "list" => Map(queryPrefix + "limit" -> limit)
      case "prop" => generatorPrefix.fold(Map.empty[String, String])(s => Map(queryPrefixWithGen + "limit" -> limit))
    }

    val params = Map("action" -> "query",
      module -> queryType) ++
      limits ++
      generator.fold(Map.empty[String, String])(s => Map("generator" -> s)) ++
      toMap(queryParamNames) ++
      (if (namespaces.nonEmpty) Map(queryPrefixWithGen + "namespace" -> namespaces.mkString("|")) else Map.empty) ++
      extraParams ++
      continueParam.fold(
        Map("continue" -> "")) {
        case (c1, c2) => Map("continue" -> c1, (queryPrefixWithGen + "continue") -> c2)
      }
    params
  }


}

//class PagesQuery(query: Either[Set[Int], Set[String]]) extends PageQuery(query)
class SinglePageQuery(query: Either[Long, String], site: MwBot) extends PageQuery(query.fold(id => Left(Set(id)), title => Right(Set(title))), site) {
  def whatTranscludesHere(namespaces: Set[Int] = Set.empty, continueParam: Option[(String, String)] = None): Future[Seq[Page]] = {
    queryList(namespaces, continueParam, "embeddedin", "ei")
  }

  def categoryMembers(namespaces: Set[Int] = Set.empty, continueParam: Option[(String, String)] = None): Future[Seq[Page]] = {
    queryList(namespaces, continueParam, "categorymembers", "cm")
  }

  def edit(text: String, summary: String, token: Option[String] = None, multi:Boolean = true) = {
    val fold: String = token.fold(site.token)(identity)
    val params = Map("action" -> "edit",
      "text" -> text,
      "summary" -> summary,
      "format" -> "json",
      "bot" -> "x",
      "token" -> fold) ++ toMap("pageid", "title")

    if (multi)
      site.postMultiPart(editResponseReads, params)
    else
      site.post(editResponseReads, params)
  }

  def upload(filename: String) {
    val pagename = query.right.toOption.fold(filename)(identity)
    val token = site.token
    val fileContents = Files.readAllBytes(Paths.get(filename))
    val params = Map(
      "action" -> "upload",
      "filename" -> pagename,
      "token" -> token,
      "format" -> "json",
      "comment" -> "update",
      "filesize" -> fileContents.size.toString,
      "ignorewarnings" -> "true")
    site.postFile(editResponseReads, params, "file", filename)
  }
}


object PageQuery {
  def byTitles(titles: Set[String], site: MwBot) = new PageQuery(Right(titles), site)

  def byTitle(title: String, site: MwBot) = new SinglePageQuery(Right(title), site)

  def byIds(ids: Set[Long], site: MwBot) = new PageQuery(Left(ids), site)

  def byId(id: Long, site: MwBot) = new SinglePageQuery(Left(id), site)
}

