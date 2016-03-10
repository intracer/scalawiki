package org.scalawiki.http

import java.net.URL

import spray.http.{HttpEntity, _}
import uk.co.bigbeeconsultants.http.header.{CookieJar, Headers}
import uk.co.bigbeeconsultants.http.request.RequestBody
import uk.co.bigbeeconsultants.http.{HttpClient => BeeClient}

import scala.concurrent.Future

class HttpClientBee extends HttpClient {

  val bee = new BeeClient

  val userAgent = "ScalaWiki/0.4"

  val headers = Headers(Map("User-Agent" -> userAgent))

  var cookieJar = CookieJar()

  def getBody(response: HttpResponse): String =
    response.entity.asString(HttpCharsets.`UTF-8`)

  override def get(url: String): Future[String] = {
    val response = bee.get(new URL(url), headers, cookieJar)
    cookieJar = response.cookies.getOrElse(CookieJar())
    Future.successful(response.body.asString)
  }

  override def get(url: Uri): Future[String] = {
    get(url.toString())
  }

  override def postMultiPart(url: String, params: Map[String, String]): Future[HttpResponse] = ???

  override def post(url: String, params: Map[String, String]): Future[HttpResponse] = {
    val response = bee.post(new URL(url), Some(RequestBody(params)), headers, cookieJar)
    cookieJar = response.cookies.getOrElse(CookieJar())
    val spray = HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, response.body.asBytes))
    Future.successful(spray)
  }

  override def post(url: Uri, params: Map[String, String]): Future[HttpResponse] = post(url.toString(), params)

  override def postFile(url: String, params: Map[String, String], fileParam: String, filename: String): Future[HttpResponse] = ???

  override def getResponse(url: Uri): Future[HttpResponse] = {
    getResponse(url.toString())
  }

  override def getResponse(url: String): Future[HttpResponse] = {
    val response = bee.get(new URL(url))
    val spray = HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, response.body.asBytes))
    Future.successful(spray)
  }

  override def postMultiPart(url: Uri, params: Map[String, String]): Future[HttpResponse] = ???

  override def setCookies(cookies: Seq[HttpCookie]): Unit = {}

  override def cookiesAndBody(response: HttpResponse): CookiesAndBody = {
    CookiesAndBody(List.empty[HttpCookie], getBody(response))
  }
}
