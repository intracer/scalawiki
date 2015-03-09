package org.scalawiki.dto

import org.scalawiki.dto.Page.Id
import org.apache.commons.codec.digest.DigestUtils
import org.joda.time.DateTime

case class Revision(
                     revId: Option[Id] = None,
                     parentId: Option[Id] = None,
                     user: Option[String] = None,
                     userId: Option[Id] = None,
                     timestamp: Option[DateTime] = None,
                     comment: Option[String] = None,
                     content: Option[String] = None,
                     size: Option[Int] = None,
                     sha1: Option[String] = None) {
  def id = revId

  def withContent(content: String*) = copy(content = Some(content.mkString("\n")))

  def withText(text: String*) = copy(content = Some(text.mkString("\n")))

  def withIds(revId: Id, parentId: Id = 0) = copy(revId = Some(revId), parentId = Some(parentId))

  def withUser(userId: Id, user: String) = copy(userId = Some(userId), user = Some(user))

  def withComment(comment: String) = copy(comment = Some(comment))

  def withTimeStamp(timestamp: DateTime = DateTime.now) = copy(timestamp = Some(timestamp))
}

object Revision {

  def create(texts: String*) = texts
    .zip(texts.size to 1 by -1)
    .map{ case (text, index) =>
    new Revision(
      revId = Some(index),
      parentId = Some(index - 1),
      content = Some(text),
      size = Some(text.size),
      sha1 = Some(DigestUtils.shaHex(text))
    )
  }
}
