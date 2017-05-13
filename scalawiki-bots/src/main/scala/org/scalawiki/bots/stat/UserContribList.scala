package org.scalawiki.bots.stat

import org.joda.time.DateTime
import org.scalawiki.query.QueryLibrary
import org.scalawiki.time.TimeRange
import org.scalawiki.{MwBot, WithBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object UserContribList extends WithBot with QueryLibrary {

  val host = MwBot.ukWiki

  def main(args: Array[String]) {

    val minEdits = 300
    val minRecentEdits = 20
    val minEditsFrom = new DateTime(2016, 10, 1, 0, 0)
    val recentEditsFrom = new DateTime(2017, 4, 1, 0, 0)

    getUsers(activeUsersQuery).foreach {
      allUsers =>

        val users = allUsers.filter { u =>
          u.editCount.exists(_ > minEdits) && u.blocked.forall(_ == false)
        }

        val contribsFuture = Future.traverse(users) { user =>
          bot.run(userContribs(user.name.get, TimeRange(Some(minEditsFrom), None), minEdits.toString))
        }

        val eligible = for (pagesSeq <- contribsFuture) yield
          for (pages <- pagesSeq if
          pages.size >= minEdits &&
            pages(minRecentEdits - 1).revisions.headOption.exists(_.timestamp.exists(_.isAfter(recentEditsFrom)));
               user <- pages.head.lastRevisionUser.flatMap(_.name)
          ) yield user

        eligible.map { u =>
          u.foreach(println)
        }.failed.map(println)
    }
  }
}