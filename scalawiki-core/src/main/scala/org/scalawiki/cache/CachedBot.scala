package org.scalawiki.cache

import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.Action
import org.scalawiki.{ActionBot, MwBot}

import scala.concurrent.Future

class CachedBot(apiBot: MwBot) extends ActionBot {
  override def run(action: Action, context: Map[String, String], limit: Option[Long]): Future[Seq[Page]] = {
    apiBot.run(action, context, limit)
  }
}
