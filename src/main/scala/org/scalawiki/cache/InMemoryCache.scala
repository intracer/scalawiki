package org.scalawiki.cache

import org.scalawiki.dto.Page.Id
import org.scalawiki.dto.{Revision, Page}


class InMemoryCache extends Cache {

  var pageMap = Map.empty[Id, Page]
  var revisionMap = Map.empty[Id, Revision]

  override def hasPage(id: Id): Boolean = pageMap.contains(id)

  override def hasRevision(id: Id): Boolean = revisionMap.contains(id)

  override def addPages(pages: Seq[Page]): Unit = {
    for (page <- pages) {

      for (rev <- page.revisions)
        revisionMap += (rev.revId -> rev)

      val updatedPage =
        if (pageMap.contains(page.id))  {
          val cached = pageMap(page.id)

          val cachedRevs = cached.revisions
          val newRevs = page.revisions
          val allRevs = cachedRevs ++ newRevs
          val allRevIds = allRevs.map(_.id).toSet.toSeq.sortBy((x:Id) => -x)

          val updatedRevs = allRevIds.map(revisionMap.apply)
          page.copy(revisions = updatedRevs)
        } else {
          page
        }

      pageMap += (page.id -> updatedPage)

    }
  }

  override def getPage(id: Id): Option[Page] = pageMap.get(id)

  override def getRevision(id: Id): Option[Revision] = revisionMap.get(id)

  override def getPages(ids: Seq[Id]): Seq[Page] = ids.flatMap(getPage)
}
