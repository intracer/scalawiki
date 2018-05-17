package org.scalawiki.mockserver

import org.specs2.specification.BeforeAfterAll

import org.mockserver.integration.ClientAndProxy.startClientAndProxy
import org.mockserver.integration.ClientAndServer.startClientAndServer

trait StubServer extends BeforeAfterAll {
  val Port = 8080
  val Host = "localhost"
  val Protocol = "http"

  import org.mockserver.integration.ClientAndProxy
  import org.mockserver.integration.ClientAndServer

  var mockServer: ClientAndServer = _
  var proxy: ClientAndProxy = _

  override def beforeAll = {
    mockServer = startClientAndServer(Port)
    proxy = startClientAndProxy(Port + 10)
  }

  override def afterAll = {
    proxy.stop()
    mockServer.stop()
  }

  def url(path: String) = s"$Protocol://$Host:$Port$path"
}