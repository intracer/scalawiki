package org.scalawiki.dto.cmd.query.list

import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.specs2.mutable.Specification

class CategoryMembersSpec extends Specification {
  val baseParams = Map(
    "action" -> "query",
    "list" -> "categorymembers"
  )

  def categoryMembers(args: CmParam[Any]*) = {
    Action(Query(ListParam(CategoryMembers(args:_*)))).pairs.toMap
  }

  "get Map" should {
    "get title" in {
      categoryMembers(CmTitle("Category:Physics")) === baseParams + ("cmtitle" -> "Category:Physics")
    }

    "get pageid" in {
      categoryMembers(CmPageId(1234L)) === baseParams + ("cmpageid" -> "1234")
    }
  }
}
