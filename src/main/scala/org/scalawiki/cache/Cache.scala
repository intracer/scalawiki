package org.scalawiki.cache

import org.scalawiki.dto.{Revision, Page}

import org.scalawiki.dto.Page.Id

trait Cache {

  def hasPage(id: Id): Boolean

  def getPage(id: Id): Option[Page]

  def getPages(ids: Seq[Id]): Seq[Page]

  def hasRevision(id: Id): Boolean

  def getRevision(id: Id): Option[Revision]

  def addPages(pages: Seq[Page])

}


