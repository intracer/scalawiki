package org.scalawiki.cache

import org.scalawiki.dto.{Page, Revision}

import scala.collection.concurrent.TrieMap


class InMemoryPageStore extends PageStore {

  var pageMap = TrieMap.empty[Long, Page]
  var revisionMap = TrieMap.empty[Long, Revision]

  override def hasPage(id: Long): Boolean = pageMap.contains(id)

  override def hasRevision(id: Long): Boolean = revisionMap.contains(id)

  override def addPages(pages: Seq[Page]): Unit = {
    for (page <- pages) {

      for (rev <- page.revisions;
          revId <- rev.id)
        revisionMap += (revId -> rev)

      for (pageId <- page.id) {
        val updatedPage =
          if (pageMap.contains(pageId)) {
            val cached = pageMap(pageId)

            val cachedRevs = cached.revisions
            val newRevs = page.revisions
            val allRevs = cachedRevs ++ newRevs
            val allRevIds = allRevs.flatMap(_.id).distinct.sortBy((id: Long) => -id)

            val updatedRevs = allRevIds.map(revisionMap.apply)
            page.copy(revisions = updatedRevs)
          } else {
            page
          }

        pageMap += (pageId -> updatedPage)
      }
    }
  }

  override def getPage(id: Long): Option[Page] = pageMap.get(id)

  override def getRevision(id: Long): Option[Revision] = revisionMap.get(id)

  override def getPages(ids: Seq[Long]): Seq[Page] = ids.flatMap(getPage)
}
