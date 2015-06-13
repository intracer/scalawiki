package org.scalawiki.util

import akka.actor.ActorSystem
import org.scalawiki.MwBot
import org.scalawiki.sql.MwDatabase

import scala.collection.mutable


trait MockBotSpec {

  def host = "uk.wikipedia.org"

  private val system: ActorSystem = ActorSystem()

  def database: Option[MwDatabase] = None

  def getBot(commands: Command*) = {
    val http = new TestHttpClient(host, mutable.Queue(commands: _*))

    val bot = new MwBot(http, system, host, database)

    bot.mwDb.foreach(_.dropTables())
    bot.mwDb.foreach(_.createTables())

    bot
  }

}
