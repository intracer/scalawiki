package client

import akka.actor.ActorSystem
import spray.http._
import scala.concurrent.Future
import spray.client.pipelining._
import spray.httpx.encoding.Gzip
import akka.event.Logging
import spray.http.HttpHeaders.{Cookie, `User-Agent`, `Accept-Encoding`}
import spray.http.HttpRequest
import spray.http.HttpResponse
import scala.concurrent.duration._


trait HttpClient {
  val timeout: Duration = 15.seconds

  def setCookies(cookies: Seq[HttpCookie])

  def get(url: String): Future[HttpResponse]

  def get(url: Uri): Future[HttpResponse]

  def post(url: String): Future[HttpResponse]

  def post(url: Uri): Future[HttpResponse]

}

class HttpClientImpl(val system: ActorSystem) extends HttpClient {


  implicit val sys = system

  import system.dispatcher

  var cookies: Seq[HttpCookie] = Seq.empty


  override def setCookies(cookies: Seq[HttpCookie]): Unit = {
    this.cookies = cookies
  }

  // execution context for futures
  val sendAndDecode: HttpRequest => Future[HttpResponse] = sendReceive ~> decode(Gzip)
  val log = Logging(system, getClass)
//  val TraceLevel = Logging.LogLevel(Logging.DebugLevel.asInt + 1)

  def submit: HttpRequest => Future[HttpResponse] = (
    addHeaders(
      Cookie(cookies),
      `Accept-Encoding`(HttpEncodings.gzip),
      `User-Agent`("ScalaMwBot/0.1")) ~>
      logRequest(log, Logging.InfoLevel)
      ~> sendReceive
      ~> decode(Gzip)// ~> logResponse(log, Logging.InfoLevel)
    )

  override def get(url: String) = submit(Get(url))

  override def get(url: Uri) = submit(Get(url))

  override def post(url: String): Future[HttpResponse] = submit(Post(url))

  override def post(url: Uri): Future[HttpResponse] = submit(Post(url))
}

//     submit(Post(baseUrl + url, FormData(Map("username" -> user, "password" -> password)))) map cookiesAndBody
