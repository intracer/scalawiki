package org.scalawiki.xml

import org.scalawiki.cache.PageStore
import org.scalawiki.dto.{Revision, Page}


class XmlPageStore(val xmlIndex: XmlIndex) extends PageStore {

  override def hasPage(id: Long): Boolean =
    xmlIndex._byId.contains(id)

  override def getPages(ids: Seq[Long]): Seq[Page] =
    ids.flatMap(xmlIndex._byId.get).map(pi => Page(Some(pi.id), 0, pi.title))

  override def getPage(id: Long): Option[Page] = ???

  override def getRevision(id: Long): Option[Revision] = ???

  override def hasRevision(id: Long): Boolean = ???

  override def addPages(pages: Seq[Page]): Unit = ???
}
