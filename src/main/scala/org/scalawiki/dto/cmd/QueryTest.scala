package org.scalawiki.dto.cmd

import org.scalawiki.dto.cmd.query.prop.{PropParam, Info, InProp, SubjectId, Revisions}
import org.scalawiki.dto.cmd.query.Query


object QueryTest {

  def main(args: Array[String]) {

    val action = ActionParam(Query(PropParam(Info(InProp(SubjectId)), Revisions())))

     println(action.pairs)
  }
}
