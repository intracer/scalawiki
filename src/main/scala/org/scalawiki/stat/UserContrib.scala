package org.scalawiki.stat

import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.query.DslQuery
import org.scalawiki.{MwBot, WithBot}

object UserContrib extends WithBot {

  val host = MwBot.ukWiki

  import scala.concurrent.ExecutionContext.Implicits.global

  def main(args: Array[String]) {
    val action =
      Action(
        Query(
          ListParam(
            AllUsers(AuProp(Seq("registration", "editcount", "blockinfo")), AuWithEditsOnly(true), AuLimit("max"), AuExcludeGroup(Seq("bot"))))
        )
      )

    new DslQuery(action, bot).run().map {
      pages =>
        val users = pages.flatMap(_.lastRevisionUser).filter { u =>
          u.editCount.exists(_ > 300) && u.blocked.forall(_ == false)
        }.flatMap(_.name)

        users.foreach(println)
    }
  }
}
