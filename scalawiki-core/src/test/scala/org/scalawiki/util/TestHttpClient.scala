package org.scalawiki.util

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import org.scalawiki.http.HttpClient
import org.specs2.matcher.Matchers

import java.nio.charset.StandardCharsets
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.language.postfixOps

class TestHttpClient(val host: String, commandsParam: Seq[HttpStub]) extends Matchers with HttpClient {

  implicit val sys: ActorSystem = ActorSystem()

  private val commands = mutable.Queue(commandsParam: _*)

  override def getResponse(url: String): Future[HttpResponse] = getResponse(Uri(url))

  override def getResponse(url: Uri): Future[HttpResponse] = getResponse(url, url.query().toMap)

  def getResponse(url: Uri, params: Map[String, String]): Future[HttpResponse] = {
    require(commands.nonEmpty, "Unexpected query: " + url.toString() + " with params:\n" + params)

    val command = commands.dequeue()

    require(url.scheme == "https")
    require(url.authority.host.address == host)
    require(url.path.toString() == command.path)

    val matchResult = params === command.query
    require(matchResult.isSuccess, matchResult.message)

    val pageResponse = Option(command.response)
      .fold(HttpResponse(StatusCodes.NotFound))(
        text => HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(command.contentType, text.getBytes(StandardCharsets.UTF_8))
        )
      )

    Promise.successful(pageResponse).future
  }

  override def post(url: String, params: Map[String, String]): Future[HttpResponse] = getResponse(url, params)

  override def postUri(url: Uri, params: Map[String, String]): Future[HttpResponse] = getResponse(url, params)

  override def postMultiPart(url: String, params: Map[String, String]): Future[HttpResponse] = getResponse(url, params)

  override def postFile(url: String, params: Map[String, String], fileParam: String, filename: String): Future[HttpResponse] = ???

  override def get(url: String): Future[String] = getResponse(url) flatMap getBody

  override def get(url: Uri): Future[String] = getResponse(url) flatMap getBody

  override def getBody(response: HttpResponse): Future[String] =
    response.entity.toStrict(5 minutes).map(_.data.utf8String)

  override def postMultiPart(url: Uri, params: Map[String, String]): Future[HttpResponse] = ???

}
