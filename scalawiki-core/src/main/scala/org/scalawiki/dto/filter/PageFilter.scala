package org.scalawiki.dto.filter

import org.scalawiki.dto.Page

object PageFilter {

  type PageFilter = Page => Boolean

  def titles(titles: Set[String]): PageFilter = (p: Page) => titles.contains(p.title)

  def ids(ids: Set[Long]): PageFilter = (p: Page) => p.id.exists(ids.contains)

  def ns(namespaces: Set[Int]): PageFilter = (p: Page) => p.ns.exists(namespaces.contains)

  val all: PageFilter = _ => true

  val none: PageFilter = _ => false

}
