package org.scalawiki.query

import org.scalawiki.dto.Page
import org.scalawiki.json.MwReads._
import org.scalawiki.json.MwReads2
import org.scalawiki.{MwBot, MwUtils}
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.Future

class PageQueryImplV1(query: Either[Set[Long], Set[String]], site: MwBot) extends PageQuery {

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

  protected def queryList(namespaces: Set[Int] = Set.empty, continueParam: Option[(String, String)], queryType: String, queryPrefix: String) = {
    query(namespaces, continueParam, "list", queryType, queryPrefix)
  }

  protected def queryProps(
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

  protected def queryByGenerator(generator: String, generatorPrefix: String,
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

  protected def query(
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

//        future { pages }
        continue.fold( Future {
          val result = previousPages ++ pages
          site.system.log.info(s"""query: ${toMap("id", "title")} , size: ${result.size}""")
          result
        }) { c =>
          query(namespaces, Some(c.continue.get, c.prefixed.get), module, queryType, queryPrefix, limit, extraParams, generator, generatorPrefix, previousPages ++ pages)
        }
    }
  }

  protected def makeParams(namespaces: Set[Int],
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






