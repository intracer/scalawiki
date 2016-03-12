package org.scalawiki.cache

import org.scalawiki.dto.{Page, Revision}


trait PageStore {

  def hasPage(id: Long): Boolean

  def getPage(id: Long): Option[Page]

  def getPages(ids: Seq[Long]): Seq[Page]

  def hasRevision(id: Long): Boolean

  def getRevision(id: Long): Option[Revision]

  def addPages(pages: Seq[Page])

}


