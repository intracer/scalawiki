package org.scalawiki.http

import java.io._
import java.net.URL
import java.nio.channels.Channels

import akka.actor.ActorSystem
import akka.event.Logging
import akka.util.Timeout
import net.spraycookies.tldlist.DefaultEffectiveTldList
import net.spraycookies.{CookieHandling, CookieJar}
import spray.client.pipelining._
import spray.http.HttpHeaders.{`Accept-Encoding`, `User-Agent`}
import spray.http._
import spray.httpx.encoding.Gzip
import spray.httpx.marshalling.Marshaller

import scala.concurrent.Future
import scala.concurrent.duration._

class HttpClientSpray(val system: ActorSystem) extends HttpClient {

  implicit val sys = system

  val userAgent = "ScalaWiki/0.4"

  import system.dispatcher

  // execution context for futures
  val sendAndDecode: HttpRequest => Future[HttpResponse] = sendReceive ~> decode(Gzip)
  val log = Logging(system, getClass)
  //  val TraceLevel = Logging.LogLevel(Logging.DebugLevel.asInt + 1)

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
          logRequest(log, Logging.InfoLevel)
            ~> sendReceive
            ~> decode(Gzip))
        ~> logResponse(r => log.debug(s"HttpResponse: ${r.status}, ${r.headers}"))
      )
  }

  override def get(url: String) = submit(Get(url)) map getBody

  override def get(url: Uri) = submit(Get(url)) map getBody

  override def getResponse(url: Uri): Future[HttpResponse] = submit(Get(url))

  override def getResponse(url: String): Future[HttpResponse] = submit(Get(url))

  implicit val UTF8FormDataMarshaller =
    Marshaller.delegate[FormData, String](MediaTypes.`application/x-www-form-urlencoded`) { (formData, contentType) ⇒
      Uri.Query(formData.fields: _*).render(new StringRendering, HttpCharsets.`UTF-8`.nioCharset).get
    }

  override def post(url: String, params: Map[String, String]): Future[HttpResponse] = {
    submit(Post(url, FormData(params)))
  }

  override def post(url: Uri, params: Map[String, String]): Future[HttpResponse] = {
    submit(Post(url, FormData(params)))
  }

  override def postMultiPart(url: String, params: Map[String, String]): Future[HttpResponse] = {
    val bodyParts = params.map { case (key, value) =>
      (key, BodyPart(HttpEntity(value), key))
    }
    submit(Post(Uri(url), new MultipartFormData(bodyParts.values.toSeq)))
  }

  override def postMultiPart(url: Uri, params: Map[String, String]): Future[HttpResponse] = {
    val bodyParts = params.map { case (key, value) =>
      (key, BodyPart(HttpEntity(value), key))
    }
    submit(Post(url, new MultipartFormData(bodyParts.values.toSeq)))
  }


  override def postFile(url: String, params: Map[String, String], fileParam: String, filename: String): Future[HttpResponse] = {

    val bodyParts = params.map { case (key, value) =>
      (key, BodyPart(HttpEntity(value), key))
    } + (fileParam -> BodyPart(new File(filename), fileParam))
    submit(Post(Uri(url), new MultipartFormData(bodyParts.values.toSeq)))
  }

  def download(urlStr: String, filename: String): Unit = {
    val url = new URL(urlStr)

    val rbc = Channels.newChannel(url.openStream())
    val fos = new FileOutputStream(filename)
    fos.getChannel.transferFrom(rbc, 0, Long.MaxValue)
    fos.close()
    rbc.close()
  }

  val BUFFER_SIZE = 8192

  def downloadToStream(url: String, outputStream: OutputStream) = {
    copy(new URL(url).openStream(), outputStream)
  }

  def copy(source: InputStream, sink: OutputStream): Long = {
    var nread: Long = 0L
    val buf: Array[Byte] = new Array[Byte](BUFFER_SIZE)
    var n: Int = 0
    while ( {
      n = source.read(buf)
      n
    } > 0) {
      sink.write(buf, 0, n)
      nread += n
    }
    nread
  }

  def getBody(response: HttpResponse): String =
    response.entity.asString(HttpCharsets.`UTF-8`)
}