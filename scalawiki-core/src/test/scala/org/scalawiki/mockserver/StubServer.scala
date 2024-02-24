package org.scalawiki.mockserver

import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.specs2.specification.BeforeAfterEach

trait StubServer extends BeforeAfterEach {
  val Port = 8080
  val Host = "localhost"
  val Protocol = "http"

  import org.mockserver.integration.ClientAndServer

  var mockServer: ClientAndServer = _

  override def before = {
    mockServer = startClientAndServer(Port)
  }

  override def after = {
    mockServer.stop()
  }

  def url(path: String) = s"$Protocol://$Host:$Port$path"
}
