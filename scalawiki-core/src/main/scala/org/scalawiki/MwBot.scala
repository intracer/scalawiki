package org.scalawiki

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.io.IO
import akka.pattern.ask
import org.scalawiki.dto.{LoginResponse, Page, Site}
import org.scalawiki.dto.cmd.Action
import org.scalawiki.http.{HttpClient, HttpClientAkka}
import org.scalawiki.json.MwReads._
import org.scalawiki.query.{DslQuery, PageQuery, SinglePageQuery}
import play.api.libs.json._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.coding.Gzip
import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{HttpEncodings, `Accept-Encoding`, `User-Agent`}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import net.spraycookies.{CookieHandling, CookieJar}
import net.spraycookies.tldlist.DefaultEffectiveTldList

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait MwBot {

  def host: String

  def login(user: String, password: String): Future[String]

  def run(action: Action): Future[Seq[Page]]

  def get(params: Map[String, String]): Future[String]

  def post(params: Map[String, String]): Future[String]

  def getByteArray(url: String): Future[Array[Byte]]

  def post[T](reads: Reads[T], params: (String, String)*): Future[T]

  def post[T](reads: Reads[T], params: Map[String, String]): Future[T]

  def postMultiPart[T](reads: Reads[T], params: Map[String, String]): Future[T]

  def postFile[T](reads: Reads[T], params: Map[String, String], fileParam: String, filename: String): Future[T]

  def page(title: String): SinglePageQuery

  def page(id: Long): SinglePageQuery

  def pageText(title: String): Future[String]

  def token: String

  def await[T](future: Future[T]): T

  def system: ActorSystem

  def log: LoggingAdapter
}

class MwBotImpl(val site: Site, val http: HttpClient, val system: ActorSystem) extends MwBot {

  def this(host: String, http: HttpClient, system: ActorSystem) = this(Site.host(host), http, system)

  implicit val sys = system

  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  def host = site.domain

  val baseUrl: String = site.protocol + "://" + host + site.scriptPath

  val indexUrl = baseUrl + "/index.php"

  val apiUrl = baseUrl + "/api.php"

  def encodeTitle(title: String): String = MwUtils.normalize(title)

  override def log = system.log

  override def login(user: String, password: String): Future[String] = {
    require(user != null, "User is null")
    require(password != null, "Password is null")

    log.info(s"$host login user: $user")

    tryLogin(user, password).flatMap {
      loginResponse =>
        tryLogin(user, password, loginResponse.token).map(_.result)
    }
  }

  def tryLogin(user: String, password: String, token: Option[String] = None): Future[LoginResponse] = {
    val loginParams = Map(
      "action" -> "login", "lgname" -> user, "lgpassword" -> password, "format" -> "json"
    ) ++ token.map("lgtoken" -> _)
    http.post(apiUrl, loginParams).flatMap (http.getBody) map { body =>
      val jsResult = Json.parse(body).validate(loginResponseReads)
      if (jsResult.isError) {
        throw new RuntimeException("Parsing failed: " + jsResult)
      } else {
        jsResult.get
      }
    }
  }

  override lazy val token = await(getToken)

  def getToken = get(tokenReads, "action" -> "query", "meta" -> "tokens")

  def getTokens = get(tokensReads, "action" -> "tokens")

  override def run(action: Action): Future[Seq[Page]] = {
    new DslQuery(action, this).run()
  }

  def get[T](reads: Reads[T], params: (String, String)*): Future[T] =
    http.get(getUri(params: _*)) map {
      body =>
        Json.parse(body).validate(reads).get
    }

  override def getByteArray(url: String): Future[Array[Byte]] =
    http.getResponse(url) flatMap {
      response => response.entity.toStrict(5 minutes).map(_.data.toArray)
    }

  override def post[T](reads: Reads[T], params: (String, String)*): Future[T] =
    post(reads, params.toMap)

  override def post[T](reads: Reads[T], params: Map[String, String]): Future[T] =
    http.post(apiUrl, params) flatMap http.getBody map {
      body =>
        val result = Json.parse(body).validate(reads).get
        println(result)
        result
    }

  override def postMultiPart[T](reads: Reads[T], params: Map[String, String]): Future[T] =
    http.postMultiPart(apiUrl, params) flatMap http.getBody map {
      body =>
        val json = Json.parse(body)
        val response = json.validate(reads)
        //        response.fold[T](err => {
        //          json.validate(errorReads)
        //        },
        //          success => success
        //        )
        val result = response.get
        println(result)
        result
    }

  override def postFile[T](reads: Reads[T], params: Map[String, String], fileParam: String, filename: String): Future[T] =
    http.postFile(apiUrl, params, fileParam, filename) flatMap http.getBody map {
      body =>
        val json = Json.parse(body)
        val response = json.validate(reads)
        //        response.fold[T](err => {
        //          json.validate(errorReads)
        //        },
        //          success => success
        //        )
        val result = response.get
        println(result)
        result
    }

  def pagesByTitle(titles: Set[String]) = PageQuery.byTitles(titles, this)

  def pagesById(ids: Set[Long]) = PageQuery.byIds(ids, this)

  override def page(title: String) = PageQuery.byTitle(title, this)

  override def page(id: Long) = PageQuery.byId(id, this)

  override def pageText(title: String): Future[String] = {
    val url = getIndexUri("title" -> encodeTitle(title), "action" -> "raw")
    http.get(url)
  }

  def getIndexUri(params: (String, String)*) =
    Uri(indexUrl) withQuery (Query(params ++ Seq("format" -> "json"): _*))

  def getUri(params: (String, String)*) =
    Uri(apiUrl) withQuery (Query(params ++ Seq("format" -> "json"): _*))

  override def get(params: Map[String, String]): Future[String] = {
    val uri: Uri = getUri(params)

    log.info(s"$host GET url: $uri")
    http.get(uri)
  }

  override def post(params: Map[String, String]): Future[String] = {
    val uri: Uri = Uri(apiUrl)
    log.info(s"$host POST url: $uri, params: $params")
    http.post(uri, params ++ Map("format" -> "json")) flatMap http.getBody
  }

  def getUri(params: Map[String, String]) =
    Uri(apiUrl) withQuery (Query(params ++ Map("format" -> "json")))

  override def await[T](future: Future[T]) = Await.result(future, http.timeout)
}

object MwBot {

  import spray.caching.{Cache, LruCache}

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent._

  val commons = "commons.wikimedia.org"
  val ukWiki = "uk.wikipedia.org"
  val useSpray = true

  def create(host: String, withDb: Boolean = false): MwBot = {
    val system = ActorSystem()
    val http = new HttpClientAkka(system)

    val bot = new MwBotImpl(host, http, system)

    bot.await(bot.login(LoginInfo.login, LoginInfo.password))
    bot
  }

  val cache: Cache[MwBot] = LruCache()

  def get(host: String): MwBot = {
    Await.result(cache(host) {
      Future {
        create(host)
      }
    }, 1.minute)
  }
}


