package client.dto

import client.dto.Page.Id
import org.joda.time.DateTime

class History(val page: Page) {

  def revisions = page.revisions

  def users(from: Option[DateTime] = None, to: Option[DateTime] = None): Set[String] = {
    val filtered = new HistoryFilter(from, to).apply(revisions)
    filtered.flatMap(_.user).toSet
  }

  def delta(from: Option[DateTime], to: Option[DateTime] = None): Option[Int] = {
    val filtered = new HistoryFilter(from, to).apply(revisions)
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

class HistoryFilter(
                     val from: Option[DateTime] = None,
                     val to: Option[DateTime] = None,
                     val user: Option[String] = None,
                     val userId: Option[Id] = None) {

  def apply(revisions: Seq[Revision]): Seq[Revision] = {
    revisions.filter {
      rev =>
        rev.timestamp.forall(ts => from.forall(ts.isAfter) && to.forall(ts.isBefore)) &&
        rev.user.forall(u => user.forall(u.equals)) &&
        rev.userId.forall(u => userId.forall(u.equals))
    }
  }
}
