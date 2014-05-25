package client.medialaw

import spray.http._
import spray.client.pipelining._
import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.event.Logging
import spray.httpx.encoding.Gzip
import spray.http.HttpHeaders.{Cookie, `Set-Cookie`}
import spray.http.HttpRequest
import spray.http.HttpResponse
import scala.util.matching.Regex


object ScalaProject extends App {

  println("Hello, Scala Project")

  implicit val system = ActorSystem()

  import system.dispatcher

  // execution context for futures
  val sendAndDecode: HttpRequest => Future[HttpResponse] = sendReceive ~> decode(Gzip)

  // val pipeline:  ~> sendReceive ~> decode(Gzip)
  val log = Logging(system, getClass)
  val baseUrl: String = "http://medialaw.kiev.ua/support/"

  get()

  def get() {
    login() map {
      response: CookiesAndBody =>
        val withCookies: HttpRequest => Future[HttpResponse] = (
          addHeader(Cookie(response.cookies))
            ~> sendReceive
            ~> decode(Gzip)
          )

        val page=1
        getTablePage(withCookies, page)
    }
  }


  def getTablePage(withCookies: (HttpRequest) => Future[HttpResponse], page: Int): Future[Unit] = {
    def news() = withCookies(Get(baseUrl + s"news.php?_pn=$page")) map getBody
    news map parseTablePage
  }

  def parseTablePage(body: String) {
    val articleIdRegex = new Regex("href=\"\\?key_id=(\\d+)\"")
    val pageIdRegex = new Regex("href=\"\\?_pn=(\\d+)\"")

    // println(body)

    def getArticleIds(text: String): Iterator[String] = for (articleIdRegex(id) <- articleIdRegex findAllIn text) yield id
    def getPageIds(text: String): Iterator[String] = for (pageIdRegex(id) <- pageIdRegex findAllIn text) yield id
    getArticleIds(body) foreach println
  }

  def login(url: String = "", user: String = "elena", password: String = "******"): Future[CookiesAndBody] = {
    val submit: HttpRequest => Future[HttpResponse] = (
        logRequest(log, Logging.InfoLevel)
        ~> sendReceive
        ~> decode(Gzip))

    log.info("login")

    submit(Post(baseUrl + url, FormData(Map("username" -> user, "password" -> password)))) map cookiesAndBody
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

  def shutdown(): Unit = {
    //      IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }

}


//    responseFuture onComplete {
//      case Success(response) =>
//        //        println(response)
//
//        httpCookie = response.headers.collect {
//          case `Set-Cookie`(hc) => hc
//        }
//
//        println(response.entity.asString(HttpCharsets.`UTF-8`))
//        shutdown()
//
//      case Failure(error) =>
//        println(error)
//        shutdown()
//    }
//

