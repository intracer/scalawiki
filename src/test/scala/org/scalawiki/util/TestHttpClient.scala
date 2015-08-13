package org.scalawiki.util

import org.scalawiki.http.{CookiesAndBody, HttpClient}
import org.specs2.matcher.Matchers
import spray.http.{HttpResponse, _}

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

class TestHttpClient(val host: String, commandsParam: Seq[Command]) extends Matchers with HttpClient {

  val commands = mutable.Queue(commandsParam: _*)

  override def getResponse(url: String) = getResponse(Uri(url))

  override def getResponse(url: Uri): Future[HttpResponse] = {

    require(commands.nonEmpty, "Unexpected query: " + url.toString())

    val command = commands.dequeue()

    url.scheme === "https"
    url.authority.host.address === host
    url.path.toString() === command.path

    url.query.toMap === command.query

    val pageResponse = Option(command.response)
      .fold(HttpResponse(StatusCodes.NotFound))(
        text => HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, text.getBytes))
      )

    Promise.successful(pageResponse).future
  }

  override def setCookies(cookies: Seq[HttpCookie]): Unit = ???

  override def post(url: String, params: Map[String, String]): Future[HttpResponse] = ???

  override def post(url: Uri, params: Map[String, String]): Future[HttpResponse] = ???

  override def postMultiPart(url: String, params: Map[String, String]): Future[HttpResponse] = ???

  override def postFile(url: String, params: Map[String, String], fileParam: String, filename: String): Future[HttpResponse] = ???

  override def get(url: String): Future[String] = getResponse(url) map getBody

  override def get(url: Uri): Future[String] = getResponse(url) map getBody

  override def getBody(response: HttpResponse): String =
    response.entity.asString(HttpCharsets.`UTF-8`)

  override def cookiesAndBody(response: HttpResponse): CookiesAndBody = ???
}
