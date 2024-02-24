package org.scalawiki.dto

import java.time.ZonedDateTime

import org.apache.commons.codec.digest.DigestUtils

case class Revision(
    revId: Option[Long] = None,
    pageId: Option[Long] = None,
    parentId: Option[Long] = None,
    user: Option[Contributor] = None,
    timestamp: Option[ZonedDateTime] = None,
    comment: Option[String] = None,
    content: Option[String] = None,
    size: Option[Long] = None,
    sha1: Option[String] = None,
    textId: Option[Long] = None
) {

//  def this(revId: Int, parentId: Option[Int] = None, user: Option[Contributor] = None, timestamp: Option[DateTime] = None,
//           comment: Option[String] = None, content: Option[String] = None,  size: Option[Int] = None,  sha1: Option[String] = None) = {
//    this(revId.toLong, parentId.map(_.toLong), user, timestamp, comment, content, size, sha1)
//  }

  def id = revId

  def isNewPage = parentId.contains(0)

  def withContent(content: String*) =
    copy(content = Some(content.mkString("\n")))

  def withText(text: String*) = copy(content = Some(text.mkString("\n")))

  def withIds(revId: Long, parentId: Long = 0) =
    copy(revId = Some(revId), parentId = Some(parentId))

  def withUser(userId: Long, login: String) =
    copy(user = Some(new User(Some(userId), Some(login))))

  def withComment(comment: String) = copy(comment = Some(comment))

  def withTimeStamp(timestamp: ZonedDateTime = ZonedDateTime.now) =
    copy(timestamp = Some(timestamp))

  def withoutContent = copy(content = None)
}

object Revision {

  def many(texts: String*) = texts
    .zip(texts.size to 1 by -1)
    .map { case (text, index) =>
      new Revision(
        revId = Some(index),
        pageId = Some(1L),
        parentId = Some(index - 1),
        content = Some(text),
        size = Some(text.length),
        sha1 = Some(DigestUtils.sha1Hex(text))
      )
    }

  def one(text: String) =
    new Revision(
      revId = None,
      pageId = None,
      parentId = None,
      content = Some(text),
      size = Some(text.length),
      sha1 = Some(DigestUtils.sha1Hex(text))
    )

  def apply(revId: Long, pageId: Long) =
    new Revision(Some(revId), Some(pageId), None)
}
