package client.dto.cmd

import client.dto.cmd.query.Query
import client.dto.cmd.query.prop._
import org.specs2.mutable.Specification

class DslSpec extends Specification {

  "12" should {
    "23" in {
      val action = ActionParam(
        Query(
          PropParam(
            Info(
              InProp(SubjectId)
            ),
            Revisions)
        )
      )

      action.pairs.toMap === Map("action" -> "query", "prop" -> "info|revisions", "inprop" -> "subjectid")

    }
  }

}
