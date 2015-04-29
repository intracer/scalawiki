package client.util

import akka.actor.ActorSystem
import org.scalawiki.MwBot
import scala.collection.mutable

trait MockBotSpec {

  val host = "uk.wikipedia.org"

  private val system: ActorSystem = ActorSystem()

  def getBot(commands: Command*) = {
    val http = new TestHttpClient(host, mutable.Queue(commands: _*))

    new MwBot(http, system, host)
  }

}
