package org.scalawiki.http

import sttp.model.Uri

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class HttpClientSttp extends HttpClient {

  import sttp.client._
  import sttp.client.asynchttpclient.future.AsyncHttpClientFutureBackend

  implicit val sttpBackend = AsyncHttpClientFutureBackend()

  def get(url: String): Future[String] = getResponse(url).map(_.body)

  def get(url: Uri): Future[String] = getResponse(url).map(_.body)

  def getResponse(url: String): Future[Response[String]] = getResponse(Uri(url))

  def getResponse(url: Uri): Future[Response[String]] = basicRequest.get(url).response(asStringAlways).send()

  def post(url: String, params: (String, String)*): Future[Response[String]] = post(url, params.toMap)

  def post(url: String, params: Map[String, String]): Future[Response[String]] = postUri(Uri(url), params)

  def postUri(url: Uri, params: Map[String, String]): Future[Response[String]] =
    basicRequest.body(params).post(url).response(asStringAlways).send()

  def postMultiPart(url: String, params: Map[String, String]): Future[Response[String]] =
    postMultiPart(Uri(url), params)

  def postMultiPart(url: Uri, params: Map[String, String]): Future[Response[String]] = {
    val multipartParams = params.map { case (k, v) => multipart(k, v) }.toSeq
    basicRequest.multipartBody(multipartParams).post(url).response(asStringAlways).send()
  }

  def postFile(url: String, params: Map[String, String], fileParam: String, filename: String, fileContents: Array[Byte]): Future[Response[String]] = {
    val multipartParams = params.map { case (k, v) => multipart(k, v) }.toSeq ++ Seq(
      multipartFile(fileParam, fileContents).fileName(filename).contentType("image/jpg")
    )

    basicRequest.multipartBody(
      multipartParams
    )
  }


  def getBody(response: Response[String]): Future[String] = Future.successful(response.body)

}

