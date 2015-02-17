package client.dto.cmd

import client.dto.cmd.query.list.{EiTitle, EmbeddedIn}
import client.dto.cmd.query.{Generator, Query}
import client.dto.cmd.query.prop._
import org.specs2.mutable.Specification

class DslSpec extends Specification {

  "1" should {
    "23" in {
      val action1 = ActionParam(
        Query(
          PropParam(
            Info(InProp(SubjectId)),
            Revisions
          )
        )
      )

      action1.pairs.toMap === Map(
        "action" -> "query",
        "prop" -> "info|revisions",
        "inprop" -> "subjectid")
    }
  }

  "2" should {
    "34" in {
      val action2 = ActionParam(
        Query(
          PropParam(
            Info(InProp(SubjectId)),
            Revisions
          ),
          Generator(EmbeddedIn(EiTitle("Template:Name")))
        )
      )

      action2.pairs.toMap === Map(
        "action" -> "query",
        "prop" -> "info|revisions",
        "inprop" -> "subjectid",
        "generator" -> "embeddedin",
        "geititle" -> "Template:Name"
      )
    }
  }


}
