package client.dto

import client.json.MwReads._
import client.json.MwReads2
import client.{MwBot, MwUtils}
import play.api.libs.json.{JsObject, JsValue, Json}
import spray.http.HttpResponse

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
                            namespaces: Set[Int] = Set(Namespace.FILE_NAMESPACE),
                            props: Set[String] = Set("timestamp", "user", "url", "size"),
                            continueParam: Option[(String, String)] = None,
                            limit:String = "max") = {
    queryByGenerator(generator, generatorPrefix, namespaces, props, continueParam, "imageinfo", "ii", limit)
  }

  def revisionsByGenerator(
                            generator: String, generatorPrefix: String,
                            namespaces: Set[Int] = Set.empty, props: Set[String] = Set.empty,
                            continueParam: Option[(String, String)] = None,
                            limit:String = "max") = {
    queryByGenerator(generator, generatorPrefix, namespaces, props, continueParam, "revisions", "rv", limit)
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
                       limit:String = "max") = {
    val extraParams: Map[String, String] = if (props.isEmpty) Map.empty else Map(queryPrefix + "prop" -> props.mkString("|"))
    query(namespaces, continueParam, "prop", queryType, queryPrefix, limit, extraParams, Some(generator), Option(generatorPrefix))
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
             previousPages: Seq[Page] = Seq.empty): Future[Seq[Page]] = {
    val params = makeParams(namespaces, continueParam, module, queryType, queryPrefix, limit, extraParams, generator, generatorPrefix)

    val url = site.getUri(params)

    import site.system.dispatcher

  import scala.concurrent._

    val eventualResponse: Future[HttpResponse] = site.http.get(url)

    eventualResponse map site.getBody flatMap {
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

        continue.fold( future {previousPages ++ pages}) { c =>
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
                  generatorPrefix: Option[String] = None): Map[String, String] = {
    val querySuffix = module match {
      case "list" => ""
      case "prop" => if (generator != Some("links")) generator.fold("s")(s => "") else "s"
    }
    val queryPrefixWithGen = generatorPrefix.fold(queryPrefix)(s => "g" + s)

    val queryParamNames = module match {
      case "list" => (queryPrefixWithGen + "pageid" + querySuffix, queryPrefixWithGen + "title" + querySuffix)
      case "prop" => (
        generatorPrefix.fold("pageid" + querySuffix)("g" + _ + "pageid"),
        generatorPrefix.fold("title" + querySuffix)("g" + _ + "title"))
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
      (if (!namespaces.isEmpty) Map(queryPrefixWithGen + "namespace" -> namespaces.mkString("|")) else Map.empty) ++
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

  def edit(text: String, summary: String, token: Option[String] = None) = {
    val fold: String = token.fold(site.token)(identity)
    val params = Map("action" -> "edit",
      "text" -> text,
      "summary" -> summary,
      "format" -> "json",
      "bot" -> "",
      "token" -> fold) ++ toMap("pageid", "title")

    site.postMultiPart(editResponseReads, params)
  }
}


object PageQuery {
  def byTitles(titles: Set[String], site: MwBot) = new PageQuery(Right(titles), site)

  def byTitle(title: String, site: MwBot) = new SinglePageQuery(Right(title), site)

  def byIds(ids: Set[Long], site: MwBot) = new PageQuery(Left(ids), site)

  def byId(id: Long, site: MwBot) = new SinglePageQuery(Left(id), site)
}

