package org.scalawiki.util

import org.scalawiki.{MwBot, MwBotImpl}

import scala.collection.mutable

trait MockBotSpec {

  def host = "uk.wikipedia.org"

  def getBot(httpStubs: HttpStub*): MwBot = {
    val http = new TestHttpClient(host, httpStubs)
    new MwBotImpl(host, http)
  }
}
