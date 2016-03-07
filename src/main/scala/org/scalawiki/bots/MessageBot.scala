package org.scalawiki.bots

import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import org.joda.time.DateTime
import org.scalawiki.MwBot
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.email._
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.dto.cmd.query.prop.{Links, PlLimit, PlNamespace, Prop}
import org.scalawiki.dto.cmd.query.{Query, TitlesParam}
import org.scalawiki.dto.{Namespace, Page, User}
import org.scalawiki.time.TimeRange
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.scalawiki.time.imports._

case class Message(subject: String, body: String)

/**
  * Send messages to users either via talk page or email
  * @param conf configuration
  */
class MessageBot(val conf: Config) {

  /**
    * Mediawiki host, e.g. en.wikipedia.org
    */
  val host = conf.getString("host")

  /**
    * Page that contains links to user pages of users we are going to notify
    */
  val userListPage = conf.getString("users.list")

  /**
    * optional start and end of time range that user contributions are queried
    */
  val (start, end) = (conf.as[Option[DateTime]]("users.start"), conf.as[Option[DateTime]]("users.end"))
  val range = TimeRange(start, end)

  /**
    * Email message
    */
  val mail = conf.as[Message]("email")

  /**
    * Talk page message
    */
  val talkPageMessage = conf.as[Message]("talk-page")

  lazy val bot = MwBot.get(host)

  def run() = {

    for (users <- fetchUsers(userListPage))
      processUsers(users, conf)
  }

  def fetchUsers(userListPage: String): Future[Seq[User]] = {
    for {
      userPages <- bot.run(whatLinksHere(userListPage, Namespace.USER))
      userInfos <- bot.run(userProps(userPagesToUserNames(userPages)))
    } yield pageUserInfos(userInfos)
  }

  def processUsers(users: Seq[User], conf: Config) = {

    val pages = users.map(u => createdPages(u.name.get, range).map(Seq(_)))
    for (createdPagesByUser <- Future.reduce(pages)(_ ++ _).map(_.toMap)) {

      val withContribution = users.filter(u => createdPagesByUser(u.name.get).nonEmpty)
      val (withEmail, withoutEmail) = withContribution.partition(_.emailable.getOrElse(false))

      logUsers(users, withEmail, withoutEmail)

      val mailedBefore = FileLines.read("emails.txt")
      val userNames = withEmail.flatMap(_.name).toSet -- mailedBefore.toSet

      messageUsers(withoutEmail, talkPageMessage)

      mailUsers(userNames, mail)
    }
  }

  def logUsers(users: Seq[User], withEmail: Seq[User], withoutEmail: Seq[User]): Unit = {
    println("AllUsers: " + users.size)
    println("WithEmail: " + withEmail.size)
    println("WithoutEmail: " + withoutEmail.size)
  }

  def messageUsers(withoutEmail: Seq[User], msg: Message): Unit = {
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

  def whatLinksHere(title: String, ns: Int) = Action(Query(
    Prop(Links(PlNamespace(Seq(ns)), PlLimit("max"))),
    TitlesParam(Seq(title))
  ))

  def userProps(users: Seq[String]) = {
    println("Users:" + users.size)
    Action(Query(
      ListParam(Users(
        UsUsers(users),
        UsProp(UsEmailable, UsGender)
      ))
    ))
  }

  def createdPages(user: String, range: TimeRange): Future[(String, Set[String])] = {
    bot.run(contribs(user, range)).map {
      pages =>
        user -> pages.filter(p => p.isArticle && p.revisions.headOption.exists(_.isNewPage)).map(_.title).toSet
    }
  }

  def contribs(user: String, range: TimeRange) = {
    val ucParams = Seq(
      UcUser(Seq(user)),
      UcDir("newer"),
      UcLimit("max")
    ) ++ range.start.map(UcStart) ++ range.end.map(UcEnd)

    Action(Query(ListParam(UserContribs(ucParams:_*))))
  }

  def userPagesToUserNames(pages: Seq[Page]): Seq[String] =
    pages.head.links.map(_.title.split("\\:").last)

  def pageUserInfos(pages: Seq[Page]) = pages.flatMap(_.lastRevisionUser).collect { case u: User => u }

  def message(user: String, section: String, text: String) = {
    bot.page("User_talk:" + user).edit(text,
      section = Some("new"),
      summary = Some(section)
    )
  }

  def email(user: String, subject: String, text: String) = {
    val cmd = Action(EmailUser(
      Target(user),
      Subject(subject),
      Text(text),
      Token(bot.token)
    ))
    bot.await(bot.post(cmd.pairs.toMap))
  }

}

object MessageBot {

  def main(args: Array[String]) {
    val conf = ConfigFactory.load("flashmob.conf").getConfig("flashmob")

    new MessageBot(conf).run()
  }
}
