package org.scalawiki.dto.history

import java.time.ZonedDateTime

import org.scalawiki.dto.Revision
import org.scalawiki.dto.filter.RevisionFilter

class History(val revisions: Seq[Revision]) {

  def hasPageCreation = revisions.headOption.exists(_.isNewPage)

  def users(revisionFilter: RevisionFilter): Set[String] = {
    val filtered = revisionFilter.apply(revisions)
    filtered.flatMap(_.user.flatMap(_.name)).toSet
  }

  def delta(revisionFilter: RevisionFilter): Option[Long] = {
    val filtered = revisionFilter.apply(revisions)
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

  def created: Option[ZonedDateTime] = revisions.lastOption.filter(_.parentId.forall(_ == 0)).flatMap(_.timestamp)

  def updated: Option[ZonedDateTime] = revisions.headOption.flatMap(_.timestamp)

  def createdAfter(from: Option[ZonedDateTime]) = created.exists(rev => from.forall(rev.isAfter))

  def editedIn(revisionFilter: RevisionFilter) =
    revisionFilter.apply(revisions).nonEmpty

}
