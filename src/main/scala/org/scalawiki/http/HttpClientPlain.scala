package org.scalawiki.http

import java.io.OutputStreamWriter
import java.net.{URL, URLConnection, URLEncoder}
import java.util.zip.GZIPInputStream

import spray.http.HttpHeaders.{RawHeader, `Set-Cookie`}
import spray.http.parser.HttpParser
import spray.http.{ContentTypes, _}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.Future
import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits.global

class HttpClientPlain extends HttpClient {

  var cookies: Seq[HttpCookie] = Seq.empty

  val zipped = true

  val userAgent = "ScalaWiki/0.4"
  val encoding = "UTF-8"
  val CONNECT_TIMEOUT_MSEC = 30000
  val READ_TIMEOUT_MSEC = 180000

  override def setCookies(cookies: Seq[HttpCookie]): Unit = {
    this.cookies ++= cookies
  }

  def cookiesAndBody(response: HttpResponse): CookiesAndBody =
    CookiesAndBody(getCookies(response), getBody(response))

  def getBody(response: HttpResponse): String =
    response.entity.asString(HttpCharsets.`UTF-8`)

  def getCookies(response: HttpResponse): List[HttpCookie] =
    response.headers.collect {
      case `Set-Cookie`(hc) => hc
    }

  override def get(url: String): Future[String] = {
    getResponse(url).map(getBody)
  }

  override def get(url: Uri): Future[String] = {
    get(url.toString())
  }

  override def postMultiPart(url: String, params: Map[String, String]): Future[HttpResponse] = ???

  def makeConnection(url: String, output: Boolean = false): URLConnection = {
    val connection = new URL(url).openConnection
    setCookies(connection)
    connection.setDoOutput(output)
    connection.setConnectTimeout(CONNECT_TIMEOUT_MSEC)
    connection.setReadTimeout(READ_TIMEOUT_MSEC)
    connection.connect()
    connection
  }

  def setCookies(u: URLConnection) {
    val cookie = cookies.mkString("; ")
//    println(s"Cookie $cookie")
    u.setRequestProperty("Cookie", cookie)
    if (zipped) u.setRequestProperty("Accept-encoding", "gzip")
    u.setRequestProperty("User-Agent", userAgent)
  }

  def grabCookies(setCookie: mutable.Buffer[String]) = {
    setCookie.map {
      str =>
        HttpParser.parseHeader(RawHeader("Set-Cookie", str)).right.get
    }.filterNot(_.value == "deleted").collect {
      case `Set-Cookie`(hc) => hc
    }
  }

  override def post(url: String, params: Map[String, String]): Future[HttpResponse] = {
    val connection = makeConnection(url, output = true)

    val request = mapToUrl(params)

    val out: OutputStreamWriter = new OutputStreamWriter(connection.getOutputStream, "UTF-8")
    try {
      out.write(request)
    } finally {
      if (out != null) out.close()
    }

    readResponse(connection)
  }

  def mapToUrl(params: Map[String, String]): String = {
   val url =  params.map { case (k, v) => s"$k=${URLEncoder.encode(v, encoding)}" }.mkString("&")
    println(url)
    url
  }

  def readResponse(connection: URLConnection) = {
    val is = if (zipped) new GZIPInputStream(connection.getInputStream) else connection.getInputStream
    val setCookie = connection.getHeaderFields.asScala.get("Set-Cookie").map(_.asScala).getOrElse(mutable.Buffer.empty[String])
    cookies ++= grabCookies(setCookie)
    val text = Source.fromInputStream(is).mkString

    val response = HttpResponse(
      StatusCodes.OK,
      HttpEntity(ContentTypes.`text/plain(UTF-8)`, text.getBytes),
      cookies.map(cookie => `Set-Cookie`(cookie)).toList
    )

    Future.successful(response)
  }

  override def post(url: Uri, params: Map[String, String]): Future[HttpResponse] = post(url.toString(), params)

  override def postFile(url: String, params: Map[String, String], fileParam: String, filename: String): Future[HttpResponse] = ???

  override def getResponse(url: Uri): Future[HttpResponse] = {
    getResponse(url.toString())
  }

  override def getResponse(url: String): Future[HttpResponse] = {
    val connection = makeConnection(url)
    readResponse(connection)
  }

  override def postMultiPart(url: Uri, params: Map[String, String]): Future[HttpResponse] = ???
}
