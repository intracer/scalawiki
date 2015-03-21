package org.scalawiki.dto.filter

import org.scalawiki.dto.Page

object PageFilter {

  def titles(titles: Set[String]) = (p:Page) => titles.contains(p.title)

  def ids(ids: Set[Page.Id]) = (p:Page) => ids.contains(p.id)

  def ns(namespaces: Set[Int]) = (p:Page) => namespaces.contains(p.ns)

  val all = (p:Page) => true

  val none = (p:Page) => false

}
