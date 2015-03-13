package org.scalawiki.dto.history

import org.joda.time.DateTime
import org.scalawiki.dto.Page.Id
import org.scalawiki.dto.{User, Revision}

class RevisionFilter(
                      val from: Option[DateTime] = None,
                      val to: Option[DateTime] = None,
                      val userName: Option[String] = None,
                      val userId: Option[Id] = None) {

  def apply(revisions: Seq[Revision]): Seq[Revision] = {
    revisions.filter(predicate)
  }

  def predicate(rev: Revision): Boolean = {
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
