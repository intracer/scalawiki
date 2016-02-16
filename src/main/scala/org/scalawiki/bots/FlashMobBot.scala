package org.scalawiki.bots

import org.joda.time.DateTime
import org.scalawiki.MwBot
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.email._
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.dto.cmd.query.prop.{Links, PlLimit, PlNamespace, Prop}
import org.scalawiki.dto.cmd.query.{Query, TitlesParam}
import org.scalawiki.dto.{Namespace, Page, User}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FlashMobBot {

  val bot = MwBot.get(MwBot.ukWiki)


  val talkPageText =
    """Доброго дня!
      |
      |Для того, щоб передати сувенір за участь у Вікіфлешмобі, нам необхідно з Вами зконтактуватися.
      |
      |Будь ласка, активуйте для цього вікіпошту: необхідно увійти до Вікіпедії під своїм логіном та паролем, перейти на сторінку [[Special:Preferences|Налаштування]](зверху сторінки),
      |підтвердити свою електронну адресу, після цього знову зайти в Налаштування, поставити галочку біля «Дозволити електронну пошту від інших користувачів» і зберегти конфігурації.
      |
      |І відпишіть організаторам на wm-ua{{@}}wikimedia.org, що Ви це зробили. Дякуємо! --~~~~
    """.stripMargin

  def emailText(user: String, url:String  = "nourl") =
    s"""Доброго дня, $user!
      |
      |Будь ласка, вкажіть як краще Вам передати сувенір за участь у Вікіфлешмобі 2016,
      |заповнивши форму за посиланням - $url
      |
      | З повагою,
      | Ілля / User:Ilya
    """.stripMargin


  def message(user: String, text: String) = {
    bot.page("User_talk:" + user).edit(text, section = Some("new"), summary = Some("Вікіфлешмоб 2016"))
  }

  def email(user: String) = {

    val subject = "Вікіфлешмоб 2016"
    val text = emailText(user)

    val cmd = Action(EmailUser(
      Target(user),
      Subject(subject),
      Text(text),
      Token(bot.token)
    ))
    bot.await(bot.post(cmd.pairs.toMap))
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

  def contribs(user: String) = {
    val start = new DateTime(2016, 1, 29, 0, 0)
    val end = new DateTime(2016, 2, 1, 0, 0)
    Action(Query(ListParam(
      UserContribs(
        UcUser(Seq(user)),
        UcStart(start),
        UcEnd(end),
        UcDir("newer"),
        UcLimit("max")
      )
    )))
  }

  def createdPages(user: String): Future[(String, Set[String])] = {

    bot.run(contribs(user)).map {
      pages =>
        user -> pages.filter(p => p.isArticle && p.revisions.headOption.exists(r => r.parentId.contains(0))).map(_.title).toSet
    }
  }

  def pageUserLinks(pages: Seq[Page]): Seq[String] =
    pages.head.links.map(_.title.split("\\:").last)

  def pageUserInfos(pages: Seq[Page]) = pages.flatMap(_.lastRevisionUser).collect { case u: User => u }

  def processUsers(users: Seq[User]) = {

    val pages = users.map(u => createdPages(u.name.get))
    val pagesMap = Future.fold(pages)(Seq.empty[(String, Set[String])])(_ :+ _).map(_.toMap)

    for (uMap <- pagesMap) {
      //         (u, uPages) <- uMap) {
      //      println(s"User $u, written ${uPages.size} pages: ${uPages.mkString(",")}")

      val (withEmail, withoutEmail) = users.filter(u => uMap(u.name.get).nonEmpty).partition(_.emailable.getOrElse(false))

      println("AllUSers: " + users.size)
      println("WithEmail: " + withEmail.size)
      println("WithoutEmail: " + withoutEmail.size)

      withoutEmail.foreach(u => message(u.name.get, talkPageText))

      withEmail.foreach { u =>
        val name = u.name.get
        val result = email(name)
        println(s" $name: $result")
      }
    }
  }

  def main(args: Array[String]) {
    val title = "Вікіпедія:Вікіфлешмоб 2016/Сувеніри" //"Користувач:RLutsBot/Редагування"

    // message("Ilya", talkPageText)
     // println(email("Ilya"))

    for {
      pages <- bot.run(whatLinksHere(title, Namespace.USER))
      userInfoPages <- bot.run(userProps(pageUserLinks(pages)))
    } {
      processUsers(pageUserInfos(userInfoPages))
    }

  }
}
