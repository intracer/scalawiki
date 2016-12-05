package org.scalawiki.query

import org.scalawiki.MwBot
import org.scalawiki.dto.{Contributor, Page}
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.{Query, TitlesParam}
import org.scalawiki.dto.cmd.query.list.{UserContribs, _}
import org.scalawiki.dto.cmd.query.meta.{EditCount, GuiUser, _}
import org.scalawiki.dto.cmd.query.prop.{Links, PlLimit, PlNamespace, Prop}
import org.scalawiki.time.TimeRange

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait QueryLibrary {

  val allUsersQuery =
    Action(Query(ListParam(
      AllUsers(
        AuProp(Seq("registration", "editcount", "blockinfo")),
        AuWithEditsOnly(true), AuLimit("max"), AuExcludeGroup(Seq("bot")))
    )))

  def userContribs(username: String, range: TimeRange, limit: String = "max", dir: String = "older") = {
    val ucParams = Seq(
      UcUser(Seq(username)),
      UcLimit(limit),
      UcDir(dir)
    ) ++ range.start.map(UcStart) ++ range.end.map(UcEnd)

    Action(Query(ListParam(UserContribs(ucParams: _*))))
  }

  def userCreatedPages(user: String, range: TimeRange)(implicit bot: MwBot): Future[(String, Set[String])] = {
    bot.run(userContribs(user, range, dir = "newer")).map {
      pages =>
        user -> pages
          .filter(p => p.isArticle && p.history.hasPageCreation)
          .map(_.title)
          .toSet
    }
  }

  def userProps(users: Seq[String]) = Action(Query(
    ListParam(Users(
      UsUsers(users),
      UsProp(UsEmailable, UsGender)
    ))
  ))

  def globalUserInfo(username: String) = Action(Query(MetaParam(
    GlobalUserInfo(
      GuiProp(
        Merged, Unattached, EditCount
      ),
      GuiUser(username)
    ))))

  def whatLinksHere(title: String, ns: Int) = Action(Query(
    Prop(Links(PlNamespace(Seq(ns)), PlLimit("max"))),
    TitlesParam(Seq(title))
  ))

  def pagesToUsers(pages: Seq[Page]) = pages.flatMap(_.lastRevisionUser)

  def getUsers(action: Action)(implicit bot: MwBot): Future[Seq[Contributor]] =
    bot.run(action).map(pagesToUsers)
}
