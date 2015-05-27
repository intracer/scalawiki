package org.scalawiki.stat

import org.joda.time.DateTime
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.query.DslQuery
import org.scalawiki.{MwBot, WithBot}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object UserContribList extends WithBot {

  val host = MwBot.ukWiki


  def main(args: Array[String]) {
    val allUsersQuery =
      Action(Query(ListParam(
        AllUsers(
          AuProp(Seq("registration", "editcount", "blockinfo")),
          AuWithEditsOnly(true), AuLimit("max"), AuExcludeGroup(Seq("bot")))
      )))

    new DslQuery(allUsersQuery, bot).run().map {
      pages =>

        val users = pages.flatMap(_.lastRevisionUser).filter { u =>
          u.editCount.exists(_ > 300) && u.blocked.forall(_ == false)
        }

        val contribsFuture = Future.traverse(users) {
          user =>
            val contribsQuery = Action(Query(ListParam(
              UserContribs(
                UcUser(Seq(user.name.get)),
                UcStart(new DateTime(2015, 4, 15, 0, 0)),
                UcLimit("300")
              )
            )))

            new DslQuery(contribsQuery, bot).run()
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
