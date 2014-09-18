package client.util

import org.specs2.mutable.Specification
import akka.actor.ActorSystem
import client.{MwBot, HttpClientImpl}
import scala.concurrent.{Future, Await}


class BaseIntegrationSpec extends Specification {

  sequential

  val ukWiki = "uk.wikipedia.org"
  val commons = "commons.wikimedia.org"
  val botUsername = "IlyaBot"
  val botPasswd = "BAGTX8uS"

  val system: ActorSystem = ActorSystem()
  val http = new HttpClientImpl(system)

  def getUkWikiBot = new MwBot(http, system, ukWiki)

  def getCommonsBot = new MwBot(http, system, commons)

  def login(wiki: MwBot, username: String = botUsername, passwd: String = botPasswd) =
    await(wiki.login(username, passwd))

  def await[T](future: Future[T]) = Await.result(future, http.timeout)

}
