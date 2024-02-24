package org.scalawiki.bots.stat

import java.time.{ZoneOffset, ZonedDateTime}

import org.scalawiki.bots.vote.VoteList
import org.scalawiki.query.QueryLibrary
import org.scalawiki.time.TimeRange
import org.scalawiki.{MwBot, WithBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object UserContribList extends WithBot with QueryLibrary {

  val host = "es.wikipedia.org"

  def main(args: Array[String]) {

    val minEdits = 300
    val minRecentEdits = 20
    val minEditsTo = ZonedDateTime.of(2017, 4, 15, 0, 0, 0, 0, ZoneOffset.UTC)
    val recentEditsFrom =
      ZonedDateTime.of(2016, 10, 15, 0, 0, 0, 0, ZoneOffset.UTC)

    for (
      votedUsers <- VoteList.votedUsers;
      allUsers <- getUsers(activeUsersQuery)
    ) {

      val users = allUsers.filter { u =>
        u.editCount.exists(_ > minEdits) && u.blocked.forall(_ == false)
      }

      val contribsFuture = Future.traverse(users) { user =>
        bot
          .run(
            userContribs(
              user.name.get,
              TimeRange(Some(minEditsTo), None),
              minEdits.toString
            ),
            limit = Some(minEdits)
          )
          .map(_.toSeq)
      }

      val eligible =
        for (pagesSeq <- contribsFuture)
          yield for (
            pages <- pagesSeq if pages.size >= minEdits &&
              pages(minRecentEdits - 1).revisions.headOption
                .exists(_.timestamp.exists(_.isAfter(recentEditsFrom)));
            user <- pages.head.lastRevisionUser.flatMap(_.name)
          ) yield user

      eligible
        .map { users =>
          users.filterNot(votedUsers.contains).foreach(println)
        }
        .failed
        .map(println)
    }
  }
}
