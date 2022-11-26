package org.scalawiki.sql.dao

import org.scalawiki.dto.{Revision, User}
import org.scalawiki.sql.{MwDatabase, Text}

import scala.concurrent.Future
import scala.language.higherKinds
import slick.driver.JdbcProfile
import spray.util.pimpFuture
import scala.concurrent.ExecutionContext.Implicits.global

class RevisionDao(val mwDb: MwDatabase, val driver: JdbcProfile) {

  import driver.api._

  val textDao = mwDb.textDao
  val userDao = mwDb.userDao

  val texts = mwDb.texts
  val revisions = mwDb.revisions

  private val autoInc = revisions returning revisions.map(_.id)

  val db = mwDb.db

  def insertAll(revisionSeq: Iterable[Revision]): Iterable[Option[Long]] = {

    val texts = revisionSeq.map(r => Text(None, r.content.getOrElse("")))
    val textIds = textDao.insertAll(texts)
    val withTextIds = addUsers(revisionSeq).zip(textIds).map {
      case (r, textId) => r.copy(textId = textId)
    }

    val hasIds = revisionSeq.head.id.isDefined
    val revIds = if (hasIds) {
      db.run(revisions.forceInsertAll(withTextIds)).await
      revisionSeq.map(_.id)
    }
    else {
      db.run(autoInc.forceInsertAll(withTextIds)).await
    }
    revIds
  }

  def insert(revision: Revision): Long = {

    val revId = if (revision.id.isDefined) {
      exists(revision.id.get) flatMap { e =>
        if (e)
          Future.successful(revision.id)
        else
          db.run(revisions.forceInsert(addUser(addText(revision)))).map(_ => revision.id)
      }
    }
    else {
      db.run(autoInc += addUser(addText(revision)))
    }
    revId.map(_.get).await
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

  def addUsers(revisionSeq: Iterable[Revision]): Iterable[Revision] = {
    val toSet = revisionSeq.toSet
    val users = toSet.flatMap(r => r.user.map(u => u.asInstanceOf[User]))

    val inDbIds = userDao.find(users.flatMap(_.id)).flatMap(_.id).toSet
    val toInsert = users.filterNot(u => inDbIds.contains(u.id.get))
    userDao.insertAll(toInsert)

    revisionSeq
  }


  def list = db.run(revisions.result).await

  def count = db.run(revisions.length.result).await

  def get(id: Long): Future[Revision] =
    db.run(revisions.filter(_.id === id).result.head)

  def exists(id: Long): Future[Boolean] =
    db.run(revisions.filter(_.id === id).exists.result)

  //    get(id).recover { case _ => false }.map(_ => true)

  def withText(id: Long): Future[Revision] =
    db.run((revisions.filter(_.id === id)
      join texts on (_.textId === _.id)).result).map { rows =>
      rows.map {
        case (r, t) => r.copy(content = Some(t.text))
      }.head
    }
}
