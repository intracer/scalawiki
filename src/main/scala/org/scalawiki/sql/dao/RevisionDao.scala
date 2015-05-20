package org.scalawiki.sql.dao

import org.scalawiki.dto.{Revision, User}
import org.scalawiki.sql.{MwDatabase, Text}

import scala.language.higherKinds
import scala.slick.driver.JdbcProfile

class RevisionDao(val mwDb: MwDatabase, val driver: JdbcProfile) {

  import driver.simple._

  val textDao = mwDb.textDao
  val userDao = mwDb.userDao

  val texts = mwDb.texts
  val revisions = mwDb.revisions

  private val autoInc = revisions returning revisions.map(_.id)

  def insertAll(revisionSeq: Seq[Revision])(implicit session: Session): Unit = {
    revisions.forceInsertAll(revisionSeq: _*)
  }

  def insert(revision: Revision)(implicit session: Session): Option[Long] = {
    val revId = if (revision.id.isDefined) {
      if (get(revision.id.get).isEmpty) {
        revisions.forceInsert(addUser(addText(revision)))
      }

      revision.id
    }
    else {
      autoInc += addUser(addText(revision))
    }
    revId
  }

  def addText(revision: Revision)(implicit session: Session): Revision = {
    val text = Text(None, revision.content.getOrElse(""))
    val textId = textDao.insert(text)
    revision.copy(textId = textId)
  }

  def addUser(revision: Revision)(implicit session: Session): Revision = {
    revision.user.fold(revision) { case user: User =>
      if (user.id.isDefined && user.login.isDefined) {
        if (userDao.get(user.id.get).isEmpty) {
          userDao.insert(user)
        }
        revision
      } else {
        if (user.id.isDefined) {
          val userId = user.id.get
          val dbUser = userDao.get(userId)
          if (dbUser.isDefined) {
            revision.copy(user = dbUser)
          } else {
            throw new IllegalArgumentException(s"No user with id $userId exists")
          }
        } else if (user.name.isDefined) {
          val username = user.name.get
          val dbUser = userDao.get(username)
          if (dbUser.isDefined) {
            revision.copy(user = dbUser)
          } else {
            throw new IllegalArgumentException(s"No user with name $username exists")
          }
        } else {
          revision
        }
      }
    }
  }

  def list(implicit session: Session) = revisions.run

  def get(id: Long)(implicit session: Session): Option[Revision] =
    revisions.filter(_.id === id).firstOption

  def withText(id: Long)(implicit session: Session): Option[Revision] =
    (revisions.filter {
      _.id === id
    } join texts on (_.textId === _.id)).run.map {
      case (r, t) => r.copy(content = Some(t.text))
    }.headOption
}
