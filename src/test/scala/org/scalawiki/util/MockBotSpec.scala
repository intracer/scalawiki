package org.scalawiki.util

import akka.actor.ActorSystem
import scala.slick.driver.H2Driver.simple._
import scala.collection.mutable
import org.scalawiki.MwBot


trait MockBotSpec {

  def host = "uk.wikipedia.org"

  private val system: ActorSystem = ActorSystem()

  def session: Session = null

  def dbCache: Boolean = Option(session).isDefined

  def getBot(commands: Command*) = {
    val http = new TestHttpClient(host, mutable.Queue(commands: _*))

    val bot = new MwBot(http, system, host, Option(session))
    Option(session).foreach { s =>
      bot.mwDb.foreach(_.dropTables()(s))
      bot.mwDb.foreach(_.createTables()(s))
    }
    bot
  }

}
