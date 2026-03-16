package org.scalawiki.wlx.actor

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.Scheduler
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.actor.typed.scaladsl.adapter._
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.util.Timeout
import org.scalawiki.MwBot
import org.scalawiki.wlx.stat.{StatParams, Statistics}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ActorStatistics {

  def main(args: Array[String]): Unit = {
    val cfg     = StatParams.parse(args)
    val contest = Statistics.getContest(cfg)
    val bot     = MwBot.fromHost(MwBot.commons)

    val contests = (cfg.years.head to cfg.years.last).map(y => contest.copy(year = y))

    val actor = bot.system.spawn(
      StatisticsActor(
        contest       = contest,
        contests      = contests,
        bot           = bot,
        persistenceId = s"${cfg.campaign}-${contest.year}"
      ),
      "statistics-actor"
    )

    implicit val scheduler: Scheduler = bot.system.toTyped.scheduler
    implicit val timeout: Timeout = 60.minutes

    val done = actor.ask[StatusReply[Done]](
      ref => GatherData(total = cfg.years.size > 1, replyTo = ref)
    )

    done.onComplete { result =>
      result.foreach(_ => println("Statistics gathering complete"))
      bot.system.terminate()
    }
  }
}
