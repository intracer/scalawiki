package org.scalawiki.http

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.coding.Gzip
import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model.{Uri, _}
import akka.http.scaladsl.model.headers.{HttpEncodings, `Accept-Encoding`, `User-Agent`}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import net.spraycookies.{CookieHandling, CookieJar}
import net.spraycookies.tldlist.DefaultEffectiveTldList

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

class HttpClientAkka(val system: ActorSystem) extends HttpClient {

  implicit val sys = system
  implicit val materializer = ActorMaterializer()

  val userAgent = "ScalaWiki/0.4"

  import system.dispatcher

  def decode: HttpResponse => HttpResponse = resp => Gzip.decode(resp)

  def sendReceive: HttpRequest => Future[HttpResponse] = req => Http().singleRequest(req)

  override implicit val timeout: Duration = 15.minutes

  val cookieJar = new CookieJar(DefaultEffectiveTldList)
  val cookied = CookieHandling.withCookies(Some(cookieJar), Some(cookieJar)) _

  def submit: HttpRequest => Future[HttpResponse] = {
    implicit val timeout: Timeout = 5.minutes
    (
      addHeaders(
        `Accept-Encoding`(HttpEncodings.gzip),
        `User-Agent`(userAgent)) ~>
        cookied(
          sendReceive
            ~> decode
        )
      )
  }

  override def get(url: String) = submit(Get(url)) flatMap getBody

  override def get(url: Uri) = submit(Get(url)) flatMap getBody

  override def getResponse(url: Uri): Future[HttpResponse] = submit(Get(url))

  override def getResponse(url: String): Future[HttpResponse] = submit(Get(url))

  override def post(url: String, params: Map[String, String]): Future[HttpResponse] = {
    submit(Post(url, FormData(params)))
  }

  override def post(url: Uri, params: Map[String, String]): Future[HttpResponse] = {
    submit(Post(url, FormData(params)))
  }

  override def postMultiPart(url: String, params: Map[String, String]): Future[HttpResponse] = {
    postMultiPart(Uri(url), params)
  }

  override def postMultiPart(url: Uri, params: Map[String, String]): Future[HttpResponse] = {
    val bodyParts = params.map { case (key, value) =>
      BodyPart(key, HttpEntity(value))
    }.toList
    submit(Post(url, Multipart.FormData(bodyParts: _*)))
  }


  override def postFile(url: String, params: Map[String, String], fileParam: String, filename: String): Future[HttpResponse] = {

    val bodyParts = params.map { case (key, value) =>
      BodyPart(key, HttpEntity(value))
    } ++ Seq(BodyPart.fromFile(fileParam, MediaTypes.`image/jpeg`, new File(filename)))
    submit(Post(Uri(url), Multipart.FormData(bodyParts.toList: _*)))
  }


  def getBody(response: HttpResponse): Future[String] =
    response.entity.toStrict(5 minutes).map(_.data.utf8String)
}