package org.scalawiki.dto.filter

import java.time.ZonedDateTime

import org.scalawiki.dto.{Revision, User}

trait RevisionFilter {

  def apply(revisions: Seq[Revision]): Seq[Revision] = {
    revisions.filter(predicate)
  }

  def predicate(rev: Revision): Boolean
}

class RevisionFilterDateAndUser(
                      val from: Option[ZonedDateTime] = None,
                      val to: Option[ZonedDateTime] = None,
                      val userName: Option[String] = None,
                      val userId: Option[Long] = None) extends RevisionFilter {

  override def predicate(rev: Revision): Boolean = {
    rev.timestamp.forall {
      ts =>
        from.forall(f => ts.isAfter(f) || ts.isEqual(f)) &&
          to.forall(t => ts.isBefore(t) || ts.isEqual(t))
    } &&
      rev.user.forall { // TODO tests
        case user: User =>
          userName.forall(user.login.equals) &&
            userId.forall(user.id.equals)
        case _ => false
      }
  }
}

object AllRevisionsFilter extends RevisionFilter {
  override def predicate(rev: Revision): Boolean = true
}