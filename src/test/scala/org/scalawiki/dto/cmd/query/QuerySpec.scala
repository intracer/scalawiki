package org.scalawiki.dto.cmd.query

import org.scalawiki.dto.cmd.query.list.EmbeddedIn
import org.scalawiki.dto.cmd.query.prop.rvprop.{Content, Ids, RvProp}
import org.scalawiki.dto.cmd.query.prop.{Info, Prop, Revisions}
import org.specs2.mutable.Specification


class QuerySpec extends Specification {

  "revision" should {
    "handle Content" in {
      val revisions = Revisions(RvProp(Ids, Content))
      revisions.hasContent === true

      val withoutContent = revisions.withoutContent
      withoutContent === Revisions(RvProp(Ids))
      withoutContent.hasContent === false
    }
  }

  "Query" should {
    "return props and revisions" in {

      val query = Query(
        Prop(
          Info(),
          Revisions(RvProp(Ids, Content))
        ))

      query.props === Seq(Info(), Revisions(RvProp(Ids, Content)))
      val revisions = query.revisions.get
      revisions === Revisions(RvProp(Ids, Content))
    }

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

      val withoutContent = query.revisionsWithoutContent

      withoutContent !=== query

      // TODO ignore order when comparing
      withoutContent === Query(
        Generator(
          new EmbeddedIn(title, None, namespaces, limit)
        ),
        Prop(
          Info(),
          Revisions(RvProp(Ids))
        )
      )
    }
  }
}