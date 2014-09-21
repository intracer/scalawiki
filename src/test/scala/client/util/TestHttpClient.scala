package client.util

import org.specs2.mutable.Specification
import spray.http._
import scala.concurrent.{Promise, Future}
import client.HttpClient
import spray.http.HttpResponse
import scala.collection.mutable

class TestHttpClient(val host: String, val commands: mutable.Queue[Command]) extends Specification with HttpClient {

  override def get(url: String) = get(Uri(url))

  override def get(url: Uri): Future[HttpResponse] = {
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
}
