package org.scalawiki.query

import org.scalawiki.dto.cmd.query.list.EmbeddedIn
import org.scalawiki.dto.cmd.query.{PageIdsParam, Generator, Query}
import org.scalawiki.dto.cmd.query.prop.rvprop.{Content, Ids, RvProp}
import org.scalawiki.dto.cmd.query.prop.{Revisions, Info, Prop}
import org.specs2.mutable.Specification

class DslQueryDbCacheSpec extends Specification {

  "cache" should {
    "return remove revision content parameter" in {

      val namespaces = Set(0)
      val limit = Some("max")
      val title = Some("Template:WLM-row")

      val query = Query(
        Prop(
          Info(),
          Revisions(RvProp(Ids, Content))
        ),
        Generator(
          new EmbeddedIn(title, None, namespaces, limit)
        )
      )

      val notInDbIds = Seq(1L, 2L, 3L)
      val notInDbQuery =  DslQueryDbCache.notInDBQuery(query, notInDbIds)

      notInDbQuery === Query(
         Prop(
          Info(),
          Revisions(RvProp(Ids, Content))
        ),
        PageIdsParam(notInDbIds)
      )
    }
  }

}
