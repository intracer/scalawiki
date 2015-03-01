package org.scalawiki.util

import org.scalawiki.http.HttpClientImpl
import org.specs2.mutable.Specification
import akka.actor.ActorSystem
import org.scalawiki.{LoginInfo, MwBot}
import scala.concurrent.{Future, Await}

class BaseIntegrationSpec extends Specification {

  sequential

  val ukWiki = "uk.wikipedia.org"
  val commons = "commons.wikimedia.org"

  val system: ActorSystem = ActorSystem()
  val http = new HttpClientImpl(system)

  def getUkWikiBot = new MwBot(http, system, ukWiki)

  def getTestBot = new MwBot(http, system, "wikilovesearth.org")

  def getCommonsBot = new MwBot(http, system, commons)

  def login(wiki: MwBot, username: String = LoginInfo.login, passwd: String = LoginInfo.password) =
    await(wiki.login(username, passwd))

  def await[T](future: Future[T]) = Await.result(future, http.timeout)

}
