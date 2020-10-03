package org.scalawiki.http

import sttp.model.Uri

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class HttpClientSttp extends HttpClient {
  import sttp.client._
  import sttp.client.asynchttpclient.future.AsyncHttpClientFutureBackend

  implicit val sttpBackend = AsyncHttpClientFutureBackend()

  def get(url: String): Future[String] =
    basicRequest.get(Uri(url)).response(asStringAlways).send().map(_.body)

  def get(url: Uri): Future[String] =
    basicRequest.get(url).response(asStringAlways).send().map(_.body)

  def getResponse(url: Uri): Future[HttpResponse]
  def getResponse(url: String): Future[HttpResponse]

  def post(url: String, params: (String, String)*): Future[HttpResponse] = post(url, params.toMap)

  def post(url: String, params: Map[String, String]): Future[HttpResponse]

  def postUri(url: Uri, params: Map[String, String]): Future[HttpResponse]

  def postMultiPart(url: String, params: Map[String, String]): Future[HttpResponse]
  def postMultiPart(url: Uri, params: Map[String, String]): Future[HttpResponse]

  def postFile(url: String, params: Map[String, String], fileParam: String, filename: String, fileContents: Array[Byte]): Future[HttpResponse]

  def getBody(response: HttpResponse): Future[String]

}

