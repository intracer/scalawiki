package org.scalawiki.http

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.coding.{Deflate, Gzip, NoCoding}
import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model.headers.{HttpEncodings, `Accept-Encoding`, `User-Agent`}
import akka.http.scaladsl.model.{Uri, _}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import net.spraycookies.tldlist.DefaultEffectiveTldList
import net.spraycookies.{CookieHandling, CookieJar}
import org.scalawiki.MwBot
import sttp.client.akkahttp.AkkaHttpBackend

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, _}
import scala.language.postfixOps

class HttpClientAkka(val system: ActorSystem = MwBot.system) extends HttpClient {

  implicit val sys = system
  implicit val materializer = ActorMaterializer()
  implicit val sttpBackend = AkkaHttpBackend.usingActorSystem(system)
  val userAgent = "ScalaWiki/0.6"

  import system.dispatcher

  def decodeResponse(response: HttpResponse): HttpResponse = {
    val decoder = response.encoding match {
      case HttpEncodings.gzip ⇒
        Gzip
      case HttpEncodings.deflate ⇒
        Deflate
      case HttpEncodings.identity ⇒
        NoCoding
    }

    decoder.decodeMessage(response)
  }

  def sendReceive: HttpRequest => Future[HttpResponse] = req => Http().singleRequest(req)

  override implicit val timeout: Duration = 15.minutes

  val cookieJar = new CookieJar(DefaultEffectiveTldList)
  val cookied = CookieHandling.withCookies(Some(cookieJar), Some(cookieJar)) _

  def submit: HttpRequest => Future[HttpResponse] = {
    implicit val timeout: Timeout = 5.minutes
    addHeaders(
      `Accept-Encoding`(HttpEncodings.gzip),
      `User-Agent`(userAgent)) ~>
      cookied(
        sendReceive
          ~> decodeResponse
      )
  }

  override def get(url: String) = submit(Get(url)) flatMap getBody

  override def get(url: Uri) = submit(Get(url)) flatMap getBody

  override def getResponse(url: Uri): Future[HttpResponse] = submit(Get(url))

  override def getResponse(url: String): Future[HttpResponse] = submit(Get(url))

  override def post(url: String, params: Map[String, String]): Future[HttpResponse] = {
    submit(Post(url, FormData(params)))
  }

  override def postUri(url: Uri, params: Map[String, String]): Future[HttpResponse] = {
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

  override def postFile(url: String, params: Map[String, String], fileParam: String, filename: String, fileContents: Array[Byte]): Future[HttpResponse] = {

    val bodyParts = params.map { case (key, value) =>
      BodyPart(key, HttpEntity(value))
    } ++ Seq(
      Multipart.FormData.BodyPart.Strict("file",
        HttpEntity(MediaTypes.`image/jpeg`, fileContents),
        Map("filename" -> filename)
      )
    )
    submit(Post(Uri(url), Multipart.FormData(bodyParts.toList: _*)))
  }

  def getBody(response: HttpResponse): Future[String] =
    response.entity.toStrict(5 minutes).map(_.data.utf8String)
}