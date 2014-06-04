package client

import spray.http._
import scala.concurrent.{ExecutionContext, Await, Future}
import spray.http.HttpResponse
import scala.concurrent.duration._
import client.dto.{Namespace, SinglePageQuery, PageQuery, Page}

import play.api.libs.json._
import client.json.MwReads._
import akka.actor.ActorSystem
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import spray.util._
import client.json.MwReads2
import spray.http.HttpHeaders.`Set-Cookie`

class MwBot(val http: HttpClient, val system: ActorSystem, val host: String) {

  implicit val sys = system

  import system.dispatcher

  val baseUrl: String = "https://" + host + "/w/"

  val indexUrl = baseUrl + "index.php"

  val apiUrl = baseUrl + "api.php"

  def encodeTitle(title: String): String = MwUtils.normalize(title)

  def log = system.log

  def login(user: String, password: String) = {
    http.post(getUri("action" -> "login", "lgname" -> user, "lgpassword" -> password)) map cookiesAndBody map { cb =>
      http.setCookies(cb.cookies)
      val json = Json.parse(cb.body)
      json.validate(loginResponseReads).fold({ err =>
        log.error("Could not login" + err)
        err.toString()
      }, { resp =>
        Await.result(http.post(getUri("action" -> "login", "lgname" -> user, "lgpassword" -> password, "lgtoken" -> resp.token)) map cookiesAndBody map { cb=>
          http.setCookies(cb.cookies)
          val json = Json.parse(cb.body)
          val l = json.validate(loginResponseReads)  // {"login":{"result":"NotExists"}}
          l.fold(err=> err.toString(),
            success => success.result
          )
        }, http.timeout)
      })
    }
  }

  def cookiesAndBody(response: HttpResponse): CookiesAndBody =
    CookiesAndBody(getCookies(response), getBody(response))

  def getBody(response: HttpResponse): String =
    response.entity.asString(HttpCharsets.`UTF-8`)

  def getCookies(response: HttpResponse): List[HttpCookie] =
    response.headers.collect {
      case `Set-Cookie`(hc) => hc
    }

  case class CookiesAndBody(cookies: List[HttpCookie], body: String)


  def pageText(title: String): Future[String] = {
    val url = getIndexUri("title" -> encodeTitle(title), "action" -> "raw")
    http.get(url) map getBody
  }

  def whatTranscludesHere(pageQuery: SinglePageQuery, namespaces: Set[Int] = Set.empty, continueParam: Option[(String, String)] = None): Future[Seq[Page]] = {
    queryList(pageQuery, namespaces, continueParam, "embeddedin", "ei")
  }

  def categoryMembers(pageQuery: SinglePageQuery, namespaces: Set[Int] = Set.empty, continueParam: Option[(String, String)] = None): Future[Seq[Page]] = {
    queryList(pageQuery, namespaces, continueParam, "categorymembers", "cm")
  }

  def revisions(pageQuery: PageQuery, namespaces: Set[Int] = Set.empty, props: Set[String] = Set.empty, continueParam: Option[(String, String)] = None) = {
    queryProps(pageQuery, namespaces, props, continueParam, "revisions", "rv") /*Set("content")*/
  }

  def imageInfoByGenerator(
                            generator: String, generatorPrefix: String,
                            query: SinglePageQuery,
                            namespaces: Set[Int] = Set(Namespace.FILE_NAMESPACE),
                            props: Set[String] = Set("timestamp", "user", "url", "size"),
                            continueParam: Option[(String, String)] = None) = {
    queryByGenerator(generator, generatorPrefix, query, namespaces, props, continueParam, "imageinfo", "ii")
  }

  def revisionsByGenerator(
                            generator: String, generatorPrefix: String,
                            query: SinglePageQuery, namespaces: Set[Int] = Set.empty, props: Set[String] = Set.empty,
                            continueParam: Option[(String, String)] = None) = {
    queryByGenerator(generator, generatorPrefix, query, namespaces, props, continueParam, "revisions", "rv")
  }



  def getIndexUri(params: (String, String)*) =
    Uri(indexUrl) withQuery (params ++ Seq("format" -> "json"): _*)

  def getUri(params: (String, String)*) =
    Uri(apiUrl) withQuery (params ++ Seq("format" -> "json"): _*)

  def getUri(params: Map[String, String]) =
    Uri(apiUrl) withQuery (params ++ Map("format" -> "json"))

  def queryList(pageQuery: PageQuery, namespaces: Set[Int] = Set.empty, continueParam: Option[(String, String)], queryType: String, queryPrefix: String) = {
    query(pageQuery, namespaces, continueParam, "list", queryType, queryPrefix)
  }

  def queryProps(
                  pageQuery: PageQuery,
                  namespaces: Set[Int] = Set.empty,
                  props: Set[String] = Set.empty,
                  continueParam: Option[(String, String)],
                  queryType: String,
                  queryPrefix: String) = {
    val extraParams: Map[String, String] = if (props.isEmpty) Map.empty else Map(queryPrefix + "prop" -> props.mkString("|"))
    query(pageQuery, namespaces, continueParam, "prop", queryType, queryPrefix, extraParams)
  }

  def queryByGenerator(generator: String, generatorPrefix: String,
                       pageQuery: PageQuery,
                  namespaces: Set[Int] = Set.empty,
                  props: Set[String] = Set.empty,
                  continueParam: Option[(String, String)],
                  queryType: String,
                  queryPrefix: String) = {
    val extraParams: Map[String, String] = if (props.isEmpty) Map.empty else Map(queryPrefix + "prop" -> props.mkString("|"))
    query(pageQuery, namespaces, continueParam, "prop", queryType, queryPrefix, extraParams, Some(generator), Some(generatorPrefix))
  }

  def query(
             pageQuery: PageQuery,
             namespaces: Set[Int] = Set.empty,
             continueParam: Option[(String, String)],
             module: String,
             queryType: String,
             queryPrefix: String,
             extraParams: Map[String, String] = Map.empty,
             generator: Option[String] = None,
             generatorPrefix: Option[String] = None): Future[Seq[Page]] = {
    val params = makeParams(pageQuery, namespaces, continueParam, module, queryType, queryPrefix, extraParams, generator, generatorPrefix)

    val url = getUri(params)

    http.get(url) map getBody map {
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
                  case "imageinfo" => MwReads2.pageWithImageInfoReads
                }
                val page = pageJson.validate(reads).get
                page
            }.toSeq
          case _ => Seq.empty
        }


        val continueParamName  = generatorPrefix.fold(queryPrefix)(s => "g" + s) + "continue"
        val continue = json.validate(continueReads(continueParamName)).asOpt

        system.log.info(s"pages: ${pages.size}, $continueParamName: $continue")

        continue.fold(pages) { c =>
          pages ++ Await.result(query(pageQuery, namespaces, Some(c.continue.get, c.prefixed.get), module, queryType, queryPrefix, extraParams, generator, generatorPrefix), http.timeout);
        }
    }
  }

  def makeParams(
                  pageQuery: PageQuery,
                  namespaces: Set[Int],
                  continueParam: Option[(String, String)],
                  module: String,
                  queryType: String,
                  queryPrefix: String,
                  extraParams: Map[String, String],
                  generator: Option[String] = None,
                  generatorPrefix: Option[String] = None): Map[String, String] = {
    val querySuffix = module match {
      case "list" => ""
      case "prop" => generator.fold("s")(s => "")
    }
    val queryPrefixWithGen = generatorPrefix.fold(queryPrefix)(s => "g" + s )

    val queryParamNames = module match {
      case "list" => (queryPrefixWithGen + "pageid" + querySuffix, queryPrefixWithGen + "title" + querySuffix)
      case "prop" => (
        generatorPrefix.fold("pageid" + querySuffix)("g" + _  + "pageid"),
        generatorPrefix.fold("title" + querySuffix)("g" + _  + "title"))
    }

    val limits = module match {
      case "list" => Map(queryPrefix + "limit" -> "max")
      case "prop" => generatorPrefix.fold(Map.empty[String, String])(s => Map(queryPrefixWithGen + "limit" -> "max"))
    }

    val params = Map("action" -> "query",
      module -> queryType) ++
      limits ++
      generator.fold(Map.empty[String, String])(s => Map("generator" -> s)) ++
      pageQuery.toMap(queryParamNames) ++
      (if (!namespaces.isEmpty) Map(queryPrefixWithGen + "namespace" -> namespaces.mkString("|")) else Map.empty) ++
      extraParams ++
      continueParam.fold(
        Map("continue" -> "")) {
        case (c1,c2) => Map("continue" -> c1, (queryPrefixWithGen + "continue") -> c2)
      }
    params
  }

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }
}

object MwBot {

  def main(args: Array[String]) {
    val system = ActorSystem()
    val http = new HttpClientImpl(system)

    import system.dispatcher

//    val ukWiki = new MwBot(http, system, "uk.wikipedia.org")
//
//    Await.result(ukWiki.login(args(0), args(1)), http.timeout)
//    listsNew(system, http, ukWiki)

    val commons = new MwBot(http, system, "commons.wikimedia.org")
    val result = Await.result(commons.login(args(0), args(1)), http.timeout)
//    images(system, http, commons)
//    imagesText(system, http, commons)
    imagesInfo(system, http, commons)


  }

  def images(system: ActorSystem, http: HttpClientImpl, commons: MwBot)(implicit dispatcher: ExecutionContext) {
    commons.categoryMembers(PageQuery.byTitle("Category:Images from Wiki Loves Earth 2014 in Ukraine")) map {
      pages =>
        system.log.info("pages:" + pages.size)

        commons.shutdown()
    }
  }

    def imagesText(system: ActorSystem, http: HttpClientImpl, commons: MwBot)(implicit dispatcher: ExecutionContext) {
      commons.revisionsByGenerator("categorymembers", "cm", PageQuery.byTitle("Category:Images from Wiki Loves Earth 2014 in Ukraine"),
        Set.empty, Set("content", "timestamp", "user", "comment")) map {
        pages =>
          system.log.info("pages:" + pages.size)

          commons.shutdown()
      }
  }

  def imagesInfo(system: ActorSystem, http: HttpClientImpl, commons: MwBot)(implicit dispatcher: ExecutionContext) {
    commons.imageInfoByGenerator("categorymembers", "cm", PageQuery.byTitle("Category:Images from Wiki Loves Earth 2014 in Ukraine")) map {
      pages =>
        system.log.info("pages:" + pages.size)

        commons.shutdown()
    }
  }

  def listsOld(system: ActorSystem, http: HttpClientImpl, ukWiki: MwBot)(implicit dispatcher: ExecutionContext) {
    ukWiki.whatTranscludesHere(PageQuery.byTitle("Template:ВЛЗ-рядок")) flatMap {
      pages =>
        system.log.info("pages:" + pages.size)
        Future.traverse(pages)(page => ukWiki.pageText(page.title).map(page.withText))
    } map {
      pages =>
        pages.foreach { page =>
          val text: String = page.text.getOrElse("")
          val monuments = "\\{\\{ВЛЗ-рядок".r.findAllMatchIn(text).size
          println(s"${page.title} - $monuments monuments")
        }

        ukWiki.shutdown()
    }
  }

  def listsNew(system: ActorSystem, http: HttpClientImpl, ukWiki: MwBot)(implicit dispatcher: ExecutionContext) {
        ukWiki.revisionsByGenerator("embeddedin", "ei", PageQuery.byTitle("Template:ВЛП-рядок"), Set.empty, Set("content", "timestamp", "user", "comment")) map {
      pages =>
        system.log.info("pages2:" + pages.size)
        for (page <- pages) {
          val text: String = page.text.getOrElse("")
          val monuments = "\\{\\{ВЛП-рядок".r.findAllMatchIn(text).size
          println(s"${page.title} - $monuments monuments")
        }

        //          val commons = new MwBot(http, system, "commons.wikimedia.org")
        ukWiki.shutdown()
    }
    // commons.shutdown()
  }


}


