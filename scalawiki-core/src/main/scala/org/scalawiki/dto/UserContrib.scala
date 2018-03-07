package org.scalawiki.dto

import java.time.ZonedDateTime

case class UserContrib(userId: Long,
                       user: String,
                       pageId: Long,
                       revId: Long,
                       parentId: Long,
                       ns: Int,
                       title: String,
                       timestamp: ZonedDateTime,
                       //                       isNew: Boolean,
                       //                       isMinor: Boolean,
                       comment: Option[String],
                       size: Option[Long]) {

  def toPage = new Page(Some(pageId), ns, title, revisions =
    Seq(new Revision(Some(revId), Some(pageId), Some(parentId), Some(User(userId, user)), Option(timestamp), comment, None, size))
  )
}
