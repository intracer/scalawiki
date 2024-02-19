package org.scalawiki.bots

import java.time.{LocalDate, ZoneOffset, ZonedDateTime}

import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import org.scalawiki.dto.{Namespace, Page, User}
import org.scalawiki.query.QueryLibrary
import org.scalawiki.time.TimeRange
import org.scalawiki.{ActionLibrary, MwBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Message(subject: String, body: String)

/** Send messages to users either via talk page or email
  *
  * @param conf
  *   configuration
  */
class MessageBot(val conf: Config) extends ActionLibrary with QueryLibrary {

  /** Mediawiki host, e.g. en.wikipedia.org
    */
  val host = conf.getString("host")

  /** Page that contains links to user pages of users we are going to notify
    */
  val userListPage = conf.getString("users.list")

  /** optional start and end of time range that user contributions are queried
    */
  val (start: Option[ZonedDateTime], end: Option[ZonedDateTime]) = (
    conf
      .as[Option[LocalDate]]("users.start")
      .map(_.atStartOfDay(ZoneOffset.UTC)),
    conf.as[Option[LocalDate]]("users.end").map(_.atStartOfDay(ZoneOffset.UTC))
  )
  val range = TimeRange(start, end)

  /** Email message
    */
  val mail = conf.as[Message]("email")

  /** Talk page message
    */
  val talkPageMessage = conf.as[Message]("talk-page")

  implicit lazy val bot: MwBot = MwBot.fromHost(host)

  def run() = {
    for (users <- fetchUsers(userListPage))
      processUsers(users, conf)
  }

  def fetchUsers(userListPage: String): Future[Iterable[User]] = {
    for {
      userPages <- bot.run(pageLinks(userListPage, Namespace.USER))
      userInfos <- bot.run(userProps(userPagesToUserNames(userPages)))
    } yield pagesToUsers(userInfos).collect { case u: User => u }
  }

  def processUsers(users: Iterable[User], conf: Config) = {

    val pages = users.map(u => userCreatedPages(u.name.get, range))
    val folded =
      Future.fold(pages)(Seq.empty[(String, Set[String])])(_ :+ _).map(_.toMap)
    for (createdPagesByUser <- folded) {

      val withContribution =
        users.filter(u => createdPagesByUser(u.name.get).nonEmpty)
      val (withEmail, withoutEmail) =
        withContribution.partition(_.emailable.getOrElse(false))

      logUsers(users, withEmail, withoutEmail)

      val mailedBefore = FileUtils.read("emails.txt")
      val userNames = withEmail.flatMap(_.name).toSet -- mailedBefore.toSet

      messageUsers(withoutEmail, talkPageMessage)

      mailUsers(userNames, mail)
    }
  }

  def logUsers(
      users: Iterable[User],
      withEmail: Iterable[User],
      withoutEmail: Iterable[User]
  ): Unit = {
    println("AllUsers: " + users.size)
    println("WithEmail: " + withEmail.size)
    println("WithoutEmail: " + withoutEmail.size)
  }

  def messageUsers(withoutEmail: Iterable[User], msg: Message): Unit = {
    withoutEmail.foreach { u =>
      val username = u.name.get
      message(username, msg.subject, msg.body)
    }
  }

  def mailUsers(toMail: Set[String], mail: Message): Unit = {
    toMail.foreach { username =>
      val result = email(username, mail.subject, mail.body.format(username))
      println(s" $username: $result")
    }
  }

  def userPagesToUserNames(pages: Iterable[Page]): Iterable[String] =
    pages.head.links.map(_.titleWithoutNs)

}

object MessageBot {

  def main(args: Array[String]) {
    val conf = ConfigFactory.load("flashmob.conf")

    new MessageBot(conf).run()
  }
}
