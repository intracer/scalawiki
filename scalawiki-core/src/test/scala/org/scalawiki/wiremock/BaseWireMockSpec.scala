package org.scalawiki.wiremock

import akka.actor.ActorSystem
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo}
import org.scalawiki.{LoginInfo, MwBot}
import org.scalawiki.http.HttpClientAkka
import org.specs2.mutable.Specification

import scala.concurrent.{Await, Future}

class BaseWireMockSpec extends Specification with StubServer {

  sequential

  val system: ActorSystem = ActorSystem()
  val http = new HttpClientAkka(system)
  val apiUrl = "/w/api.php"

  def getBot: MwBot = MwBot.fromHost(Host, Port, Protocol)

  def login(wiki: MwBot, username: String, passwd: String): String =
    await(wiki.login(username, passwd))

  def await[T](future: Future[T]): T = Await.result(future, http.timeout)

  def stubResponse(path: String, code: Int, body: String) = {
    stubFor(post(urlEqualTo(path))
      .willReturn(
        aResponse()
          .withStatus(code)
          .withBody(body)
      ))
  }

  def stubOk(params: Map[String, String], body: String) = stubResponse(params, 200, body)

  def stubResponse(params: Map[String, String], code: Int, body: String) = {

    val withParams = params.foldLeft(post(urlEqualTo(apiUrl))) { case (builder, (key, value)) =>
      builder.withQueryParam(key, WireMock.equalTo(value))
    }


    stubFor(withParams.willReturn(
      aResponse()
        .withStatus(code)
        .withBody(body)
    ))
  }
}
