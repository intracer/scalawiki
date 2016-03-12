package org.scalawiki

import akka.actor.ActorSystem
import org.scalawiki.http.HttpClientSpray
import org.specs2.mutable.Specification

import scala.concurrent.{Await, Future}

class BaseIntegrationSpec extends Specification {

  sequential

  val ukWiki = "uk.wikipedia.org"
  val commons = "commons.wikimedia.org"

  val system: ActorSystem = ActorSystem()
  val http = new HttpClientSpray(system)

  def getUkWikiBot = new MwBot(http, system, ukWiki, None)

  def getTestBot = new MwBot(http, system, "wikilovesearth.org", None)

  def getCommonsBot = new MwBot(http, system, commons, None)

  def login(wiki: MwBot, username: String = LoginInfo.login, passwd: String = LoginInfo.password) =
    await(wiki.login(username, passwd))

  def await[T](future: Future[T]) = Await.result(future, http.timeout)

}
