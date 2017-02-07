package org.scalawiki.http

import akka.http.scaladsl.model._

import scala.concurrent.Future
import scala.concurrent.duration._

trait HttpClient {
  val timeout: Duration = 30.minutes

  def get(url: String): Future[String]

  def get(url: Uri): Future[String]

  def getResponse(url: Uri): Future[HttpResponse]
  def getResponse(url: String): Future[HttpResponse]

  def post(url: String, params: (String, String)*): Future[HttpResponse] = post(url, params.toMap)

  def post(url: String, params: Map[String, String]): Future[HttpResponse]

  def postUri(url: Uri, params: Map[String, String]): Future[HttpResponse]

  def postMultiPart(url: String, params: Map[String, String]): Future[HttpResponse]
  def postMultiPart(url: Uri, params: Map[String, String]): Future[HttpResponse]

  def postFile(url: String, params: Map[String, String], fileParam: String, filename: String): Future[HttpResponse]

  def getBody(response: HttpResponse): Future[String]

}

object HttpClient {
  val JSON_UTF8 = ContentType(MediaTypes.`application/json`, Some(HttpCharsets.`UTF-8`))
}
