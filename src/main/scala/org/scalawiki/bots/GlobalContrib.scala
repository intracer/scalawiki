package org.scalawiki.bots

import org.joda.time.DateTime
import org.scalawiki.MwBot
import org.scalawiki.dto.{SulAccount, User}
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.dto.cmd.query.meta._
import org.scalawiki.time.TimeRange
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class GlobalContrib {

  val bot = MwBot.get(MwBot.commons)

  def guiAction(username: String) = Action(Query(MetaParam(
    GlobalUserInfo(
      GuiProp(
        Merged, Unattached, EditCount
      ),
      GuiUser(username)
    ))))

  def contribs(user: String, range: TimeRange) = {
    val ucParams = Seq(
      UcUser(Seq(user)),
      UcDir("newer"),
      UcLimit("500")
    ) ++ range.start.map(UcStart) ++ range.end.map(UcEnd)

    Action(Query(ListParam(UserContribs(ucParams: _*))))
  }

  def editsBefore(user: User, acc: SulAccount, start: DateTime): Future[Long] = {
    val host = acc.url.replace("https://", "")
    val siteBot = MwBot.get(host)

    val action = contribs(user.name.get, TimeRange(None, Some(start)))
    siteBot.run(action).map{ edits =>
      println(s"$host - ${edits.size}")
      edits.size.toLong
    }
  }

  def checkContribs(username: String, start: DateTime): Future[Long] = {
    bot.run(guiAction(username)).flatMap {
      pages =>
        pages.flatMap(_.lastRevisionUser.map(_.asInstanceOf[User])).headOption.fold(Future(0L)) {
          user =>
            val activeAccounts = user.sulAccounts.filter(_.editCount > 0)

            println(s"${user.name.get} - accounts ${activeAccounts.size}: ${activeAccounts.map(_.wiki)}")

            Future.traverse(activeAccounts)(acc => editsBefore(user, acc, start)).map(_.sum)
        }
    }
  }

}

object GlobalContrib {
  def main(args: Array[String]) {

    new GlobalContrib().checkContribs("Ilya", new DateTime(2016, 1, 1, 0, 0)).map {
      size =>
        println(size)
    }
  }

  def countNewComers(authors: Set[String], date: DateTime): Unit = {
    val gc = new GlobalContrib()

    Future.traverse(authors) { author =>
      gc.checkContribs(author, date).map(author -> _)
    }.map {
      set =>
        val seq = set.toSeq
        val sorted = seq.sortBy(-_._2)
        sorted.foreach(println)

        val before = seq.filter(_._2 > 0)

        println(s"Before ${before.size}")

        val newComers = seq.filter(_._2 == 0)

        println(s"newComers ${newComers.size}")
    }
  }
}
