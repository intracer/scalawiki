package client.dto.history

import client.dto.{Page, Revision}
import org.joda.time.DateTime

class History(val page: Page) {

  def revisions = page.revisions

  val annotation = Annotation.create(page)

  def users(from: Option[DateTime] = None, to: Option[DateTime] = None): Set[String] = {
    val filtered = new RevisionFilter(from, to).apply(revisions)
    filtered.flatMap(_.user).toSet
  }

  def delta(from: Option[DateTime], to: Option[DateTime] = None): Option[Int] = {
    val filtered = new RevisionFilter(from, to).apply(revisions)
    val sum = for (
      oldest <- filtered.lastOption;
      newest <- filtered.headOption;
      d1 <- delta(oldest);
      d2 <- delta(oldest, newest))
    yield d1 + d2
    sum
  }

  def delta(revision: Revision): Option[Int] =
    revision.parentId.flatMap { parentId =>
      if (parentId == 0)
        revision.size
      else
        revisions.find(_.revId.exists(_ == parentId)).flatMap {
          parent => delta(parent, revision)
        }
    }

  def delta(from: Revision, to: Revision): Option[Int] =
    for (fromSize <- from.size; toSize <- to.size) yield toSize - fromSize

  def created: Option[DateTime] = revisions.lastOption.filter(_.parentId.forall(_ == 0)).flatMap(_.timestamp)

  def updated: Option[DateTime] = revisions.headOption.flatMap(_.timestamp)

  def createdAfter(from: Option[DateTime]) = created.exists(rev => from.forall(rev.isAfter))
}
