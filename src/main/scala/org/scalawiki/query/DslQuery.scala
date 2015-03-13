package org.scalawiki.query

import org.scalawiki.MwBot
import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.ActionParam
import org.scalawiki.json.Parser

import scala.concurrent.Future

class DslQuery(action: ActionParam, site: MwBot) {

  def run: Future[Seq[Page]] = {
    val params = action.pairs ++ Seq("format" -> "json", "continue" -> "")

    import site.system.dispatcher

    import scala.concurrent._

    site.get(params.toMap) flatMap {
      body =>
        val parser = new Parser(action)

        Future {
          parser.parse(body)
        }
    }

  }
}
