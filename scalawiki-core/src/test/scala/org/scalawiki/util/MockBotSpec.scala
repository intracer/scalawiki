package org.scalawiki.util

import org.scalawiki.MwBotImpl
import scala.collection.mutable

trait MockBotSpec {

  def host = "uk.wikipedia.org"

  def getBot(commands: Command*) = {
    val http = new TestHttpClient(host, mutable.Queue(commands: _*))

    new MwBotImpl(host, http)
  }

}
