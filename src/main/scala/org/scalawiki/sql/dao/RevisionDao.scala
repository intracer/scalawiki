package org.scalawiki.sql.dao

import org.scalawiki.dto.{Revision, User}
import org.scalawiki.sql.{MwDatabase, Text}

import scala.concurrent.Future
import scala.language.higherKinds
import scala.slick.driver.JdbcProfile

class RevisionDao(val mwDb: MwDatabase, val driver: JdbcProfile) {

  import driver.api._

  val textDao = mwDb.textDao
  val userDao = mwDb.userDao

  val texts = mwDb.texts
  val revisions = mwDb.revisions

  private val autoInc = revisions returning revisions.map(_.id)

  val db = mwDb.db

  def insertAll(revisionSeq: Seq[Revision]): Unit = {
    revisions.forceInsertAll(revisionSeq)
  }

  def insert(revision: Revision): Future[Long] = {
    val revId = if (revision.id.isDefined) {
      exists(revision.id.get) flatMap { e =>
        if (e)
          db.run(revisions.forceInsert(addUser(addText(revision)))).map(_ => revision.id)
        else
          Future.successful(revision.id)
      }
    }
    else {
      db.run(autoInc += addUser(addText(revision)))
    }
    revId.map(_.get)
  }

  def addText(revision: Revision): Revision = {
    val text = Text(None, revision.content.getOrElse(""))
    val textId = textDao.insert(text)
    revision.copy(textId = textId)
  }

  def addUser(revision: Revision): Revision = {
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

  def list = db.run(revisions.result)

  def get(id: Long): Future[Revision] =
    db.run(revisions.filter(_.id === id).result.head)

  def exists(id: Long): Future[Boolean] =
    get(id).recover { case _ => false }.map(_ => true)

  def withText(id: Long): Future[Revision] =
    db.run((revisions.filter(_.id === id)
      join texts on (_.textId === _.id)).result).map { rows =>
      rows.map {
        case (r, t) => r.copy(content = Some(t.text))
      }.head
    }
}
