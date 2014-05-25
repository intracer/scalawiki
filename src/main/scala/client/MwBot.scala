package client

import spray.http._
import scala.concurrent.{ExecutionContext, Await, Future}
import spray.http.HttpResponse
import scala.concurrent.duration._
import client.dto.{SinglePageQuery, PageQuery, Page}

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
        log.error("Could not fucking login" + err)
      }, { resp =>
        http.post(getUri("action" -> "login", "lgname" -> user, "lgpassword" -> password, "lgtoken" -> resp.token)) map cookiesAndBody map { cb=>
          val json = Json.parse(cb.body)
          val l = json.validate(loginResponseReads)  // {"login":{"result":"NotExists"}}
        }
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

  def whatTranscludesHere(pageQuery: SinglePageQuery, namespaces: Set[Int] = Set.empty, continueParam: Option[String] = None): Future[Seq[Page]] = {
    queryList(pageQuery, namespaces, continueParam, "embeddedin", "ei")
  }

  def categoryMembers(pageQuery: SinglePageQuery, namespaces: Set[Int] = Set.empty, continueParam: Option[String] = None): Future[Seq[Page]] = {
    queryList(pageQuery, namespaces, continueParam, "categorymembers", "cm")
  }

  def revisions(pageQuery: PageQuery, namespaces: Set[Int] = Set.empty, props: Set[String] = Set.empty, continueParam: Option[String] = None) = {
    queryProps(pageQuery, namespaces, props, continueParam, "revisions", "rv") /*Set("content")*/
  }

  def getIndexUri(params: (String, String)*) =
    Uri(indexUrl) withQuery (params ++ Seq("format" -> "json"): _*)

  def getUri(params: (String, String)*) =
    Uri(apiUrl) withQuery (params ++ Seq("format" -> "json"): _*)

  def getUri(params: Map[String, String]) =
    Uri(apiUrl) withQuery (params ++ Map("format" -> "json"))

  def queryList(pageQuery: PageQuery, namespaces: Set[Int] = Set.empty, continueParam: Option[String], queryType: String, queryPrefix: String) = {
    query(pageQuery, namespaces, continueParam, "list", queryType, queryPrefix)
  }

  def queryProps(
                  pageQuery: PageQuery,
                  namespaces: Set[Int] = Set.empty,
                  props: Set[String] = Set.empty,
                  continueParam: Option[String],
                  queryType: String,
                  queryPrefix: String) = {
    val extraParams: Map[String, String] = if (props.isEmpty) Map.empty else Map(queryPrefix + "prop" -> props.mkString("|"))
    query(pageQuery, namespaces, continueParam, "prop", queryType, queryPrefix, extraParams)
  }

  def query(
             pageQuery: PageQuery,
             namespaces: Set[Int] = Set.empty,
             continueParam: Option[String],
             module: String,
             queryType: String,
             queryPrefix: String,
             extraParams: Map[String, String] = Map.empty): Future[Seq[Page]] = {
    val querySuffix = module match {
      case "list" => ""
      case "prop" => "s"
    }
    val queryParamNames = module match {
      case "list" => (queryPrefix + "pageid" + querySuffix, queryPrefix + "title" + querySuffix)
      case "prop" => ("pageid" + querySuffix, "title" + querySuffix)
    }

    val limits = module match {
      case "list" => Map(queryPrefix + "limit" -> "max")
      case "prop" => Map.empty
    }

    val params = Map("action" -> "query",
      module -> queryType) ++
      limits ++
      pageQuery.toMap(queryParamNames) ++
      (if (!namespaces.isEmpty) Map(queryPrefix + "namespace" -> namespaces.mkString("|")) else Map.empty) ++
      extraParams ++
      continueParam.fold(
        Map("continue" -> "")) {
        c => Map("continue" -> "-||", (queryPrefix + "continue") -> c)
      }

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
                val page = pageJson.validate(MwReads2.pageWithRevisionReads).get
                page
            }.toSeq
          case _ => Seq.empty
        }

        val continue = json.validate(continueReads(queryPrefix + "continue")).asOpt.flatten

        continue.fold(pages) { c =>
          pages ++ Await.result(query(pageQuery, namespaces, continue, module, queryType, queryPrefix), http.timeout);
        }
    }
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

    val ukWiki = new MwBot(http, system, "uk.wikipedia.org")

    Await.result(ukWiki.login(args(0), args(1)), http.timeout)
    //    val commons = new MwBot(http, system, "commons.wikimedia.org")

    listsNew(system, http, ukWiki)
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

          val commons = new MwBot(http, system, "commons.wikimedia.org")
        }
        ukWiki.shutdown()
      // commons.shutdown()
    }
  }

  def listsNew(system: ActorSystem, http: HttpClientImpl, ukWiki: MwBot)(implicit dispatcher: ExecutionContext) {
    ukWiki.whatTranscludesHere(PageQuery.byTitle("Template:ВЛП-рядок")) flatMap {
      pages =>
        system.log.info("pages:" + pages.size)
        ukWiki.revisions(PageQuery.byIds(pages.map(_.pageid.toInt).toSet), Set.empty, Set("content", "timestamp", "user", "comment"))
    } map {
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


