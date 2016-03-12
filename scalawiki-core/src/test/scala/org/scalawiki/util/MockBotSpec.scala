package org.scalawiki.util

import akka.actor.ActorSystem
import org.scalawiki.MwBotImpl
import scala.collection.mutable

trait MockBotSpec {

  def host = "uk.wikipedia.org"

  private val system: ActorSystem = ActorSystem()

  def getBot(commands: Command*) = {
    val http = new TestHttpClient(host, mutable.Queue(commands: _*))

    new MwBotImpl(http, system, host)
  }

}
