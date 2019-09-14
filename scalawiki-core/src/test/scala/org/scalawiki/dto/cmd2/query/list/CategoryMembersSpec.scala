package org.scalawiki.dto.cmd2.query.list

import org.specs2.mutable.Specification

class CategoryMembersSpec extends Specification {
  val baseParams = Map(
    "action" -> "query",
    "list" -> "categorymembers"
  )

  "get Map" should {
    "get title" in {
      CategoryMembers("Category:Physics").toMap === baseParams + ("cmtitle" -> "Category:Physics")
    }

    "get pageid" in {
      CategoryMembers(1234).toMap === baseParams + ("cmpageid" -> 1234)
    }
  }
}
