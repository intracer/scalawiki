package org.scalawiki.util

import akka.actor.ActorSystem
import scala.collection.mutable
import org.scalawiki.MwBot

trait MockBotSpec {

  val host = "uk.wikipedia.org"

  private val system: ActorSystem = ActorSystem()

  def dbCache: Boolean = false

  def getBot(commands: Command*) = {
    val http = new TestHttpClient(host, mutable.Queue(commands: _*))

    new MwBot(http, system, host, dbCache)
  }

}
