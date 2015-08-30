package org.scalawiki.dto.history

import org.joda.time.DateTime
import org.scalawiki.dto.filter.RevisionFilter
import org.scalawiki.dto.{Page, Revision}

class History(val page: Page) {

  def revisions = page.revisions

  def users(from: Option[DateTime] = None, to: Option[DateTime] = None): Set[String] = {
    val filtered = new RevisionFilter(from, to).apply(revisions)
    filtered.flatMap(_.user.flatMap(_.name)).toSet
  }

  def delta(from: Option[DateTime], to: Option[DateTime] = None): Option[Long] = {
    val filtered = new RevisionFilter(from, to).apply(revisions)
    val sum = for (
      oldest <- filtered.lastOption;
      newest <- filtered.headOption;
      d1 <- delta(oldest);
      d2 <- delta(oldest, newest))
    yield d1 + d2
    sum
  }

  def delta(revision: Revision): Option[Long] =
    revision.parentId.flatMap { parentId =>
      if (parentId == 0)
        revision.size
      else
        revisions.find(_.revId.contains(parentId)).flatMap {
          parent => delta(parent, revision)
        }
    }

  def delta(from: Revision, to: Revision): Option[Long] =
    for (fromSize <- from.size; toSize <- to.size) yield toSize - fromSize

  def created: Option[DateTime] = revisions.lastOption.filter(_.parentId.forall(_ == 0)).flatMap(_.timestamp)

  def updated: Option[DateTime] = revisions.headOption.flatMap(_.timestamp)

  def createdAfter(from: Option[DateTime]) = created.exists(rev => from.forall(rev.isAfter))

  def editedIn(from: Option[DateTime], to: Option[DateTime]) =
    new RevisionFilter(from, to).apply(revisions).nonEmpty

}
