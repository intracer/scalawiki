package org.scalawiki.stat

import org.joda.time.DateTime
import org.scalawiki.dto.Contributor
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.time.TimeRange
import org.scalawiki.{MwBot, WithBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object UserContribList extends WithBot {

  val host = MwBot.ukWiki

  val allUsersQuery =
    Action(Query(ListParam(
      AllUsers(
        AuProp(Seq("registration", "editcount", "blockinfo")),
        AuWithEditsOnly(true), AuLimit("max"), AuExcludeGroup(Seq("bot")))
    )))

  def contribsQuery(user: Contributor, range: TimeRange, limit: String) = {
    val ucParams = Seq(
      UcUser(Seq(user.name.get)),
      UcLimit(limit)
    ) ++ range.start.map(UcStart) ++ range.end.map(UcEnd)

    Action(Query(ListParam(UserContribs(ucParams: _*))))
  }

  def getUsers(action: Action): Future[Seq[Contributor]] =
    bot.run(action).map(pages => pages.flatMap(_.lastRevisionUser))

  def main(args: Array[String]) {

    getUsers(allUsersQuery).map {
      allUsers =>

        val users = allUsers.filter { u =>
          u.editCount.exists(_ > 300) && u.blocked.forall(_ == false)
        }

        val contribsFuture = Future.traverse(users) { user =>
          bot.run(contribsQuery(
            user,
            TimeRange(Some(new DateTime(2015, 4, 15, 0, 0)), None),
            "300")
          )
        }

        contribsFuture.map {
          pagesSeq =>
            val eligible = pagesSeq.filter {
              pages =>
                pages.size >= 300 &&
                  pages(19).revisions.head.timestamp.exists(
                    _.isAfter(new DateTime(2014, 10, 15, 0, 0))
                  )
            }.flatMap(_.head.lastRevisionUser.flatMap(_.name))

            eligible.foreach(println)
        }
    }
  }
}
