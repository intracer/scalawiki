package org.scalawiki.query

import org.scalawiki.MwBot
import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.Action
import org.scalawiki.json.Parser

import scala.concurrent.Future

class DslQuery(action: Action, site: MwBot) {
  import site.system.dispatcher

  def run(
           continue: Map[String, String] = Map("continue" -> ""),
           pages: Seq[Page] = Seq.empty[Page]
           ): Future[Seq[Page]] = {

      val params = action.pairs ++ Seq("format" -> "json", "bot" -> "x") ++ continue

      site.get(params.toMap) flatMap {
        body =>
          val parser = new Parser(action)

          val newPages = parser.parse(body)
          val allPages = pages ++ newPages

          val newContinue = parser.continue
          if (newContinue.isEmpty) {
            Future {allPages}
          } else {
           run(newContinue, allPages)
          }
      }
  }
}
