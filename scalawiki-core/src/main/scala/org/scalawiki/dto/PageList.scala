package org.scalawiki.dto

import scala.collection.mutable

class PageList(existing: mutable.LinkedHashMap[Long, Page] =
                 mutable.LinkedHashMap.empty,
               missing: mutable.Buffer[Page] = mutable.Buffer.empty) {

  def addPages(newPages: Seq[Page]): Unit = {
    val (newMissing, newExisting) = newPages.partition(_.id.isEmpty)
    missing.addAll(newMissing)
    for (page <- newExisting;
         id <- page.id) {
      existing(id) = existing
        .get(id)
        .map(_.appendLists(page))
        .getOrElse(page)
    }
  }

  def allPages: Iterable[Page] = {
    existing.values ++ missing
  }

  def size: Int = existing.size + missing.size

}
