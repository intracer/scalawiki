package org.scalawiki.http

import akka.actor.ActorSystem
import org.scalawiki.MwBot
import sttp.client.Response
import sttp.model.Uri

import scala.concurrent.Future
import scala.concurrent.duration._

trait HttpClient {
  val timeout: Duration = 30.minutes

  def get(url: String): Future[String]

  def get(url: Uri): Future[String]

  def getResponse[T](url: Uri): Future[Response[T]]
  def getResponse[T](url: String): Future[Response[T]]

  def post[T](url: String, params: (String, String)*): Future[Response[T]] = post(url, params.toMap)

  def post[T](url: String, params: Map[String, String]): Future[Response[T]]

  def postUri[T](url: Uri, params: Map[String, String]): Future[Response[T]]

  def postMultiPart[T](url: String, params: Map[String, String]): Future[Response[T]]
  def postMultiPart[T](url: Uri, params: Map[String, String]): Future[Response[T]]

  def postFile[T](url: String, params: Map[String, String], fileParam: String, filename: String, fileContents: Array[Byte]): Future[Response[T]]

  def getBody[T](response: Response[T]): Future[String]

}

object HttpClient {
  val JSON_UTF8 = ContentType(MediaTypes.`application/json`)

  def get(system: ActorSystem = MwBot.system): HttpClient = new HttpClientAkka(system)
}