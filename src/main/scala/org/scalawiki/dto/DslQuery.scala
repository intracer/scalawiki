package org.scalawiki.dto

import org.scalawiki.dto.cmd.ActionParam
import org.scalawiki.json.Parser
import org.scalawiki.MwBot

import scala.concurrent.Future

class DslQuery(action: ActionParam, site: MwBot) {

  def run: Future[Seq[Page]] = {
    val params = action.pairs ++ Seq("format" -> "json", "continue" -> "")

    import site.system.dispatcher

    import scala.concurrent._

    site.get(params.toMap) flatMap {
      body =>
        val parser = new Parser(action)

        future {
          parser.parse(body)
        }
    }

  }
}
