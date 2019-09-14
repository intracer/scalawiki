package org.scalawiki.dto.cmd2.query.list

trait WithNamespace {

}

case class CategoryMembers(pageRef: PageRef) {
  val baseParams = Map(
    "action" -> "query",
    "list" -> "categorymembers"
  )

  def toMap: Map[String, Any] = {
    baseParams + pageRef.toMap("cm")
  }
}

object CategoryMembers {
  val props = Set("ids", "title", "sortkey", "sortkeyprefix", "type", "timestamp")

  def apply(title: String): CategoryMembers = CategoryMembers(PageRef(title))

  def apply(pageId: Long): CategoryMembers = CategoryMembers(PageRef(pageId))
}

case class PageRef(query: Either[String, Long]) {
  def toMap(prefix: String): (String, Any) = {
    query match {
      case Left(title) => prefix + "title" -> title
      case Right(pageId) => prefix + "pageid" -> pageId
    }
  }
}

object PageRef {
  def apply(title: String): PageRef = PageRef(Left(title))

  def apply(pageId: Long): PageRef = PageRef(Right(pageId))
}