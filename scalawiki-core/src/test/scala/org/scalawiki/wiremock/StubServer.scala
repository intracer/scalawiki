package org.scalawiki.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.specs2.specification.BeforeAfterAll

trait StubServer extends BeforeAfterAll {
  val Port = 8080
  val Host = "localhost"
  val Protocol = "http"
  val wireMockServer = new WireMockServer(wireMockConfig().port(Port))

  override def beforeAll = {
    wireMockServer.start()
    WireMock.configureFor(Host, Port)
  }

  override def afterAll = wireMockServer.stop()

  def url(path: String) = s"$Protocol://$Host:$Port$path"
}