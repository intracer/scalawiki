package org.scalawiki.dto.filter

import org.scalawiki.dto.Page

object PageFilter {

  def titles(titles: Set[String]) = (p:Page) => titles.contains(p.title)

  def ids(ids: Set[Long]) = (p:Page) => p.id.exists(ids.contains)

  def ns(namespaces: Set[Int]) = (p:Page) => p.ns.exists(namespaces.contains)

  val all = (p:Page) => true

  val none = (p:Page) => false

}
