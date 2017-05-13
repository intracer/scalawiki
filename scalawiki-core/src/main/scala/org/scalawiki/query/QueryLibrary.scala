package org.scalawiki.query

import org.scalawiki.MwBot
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.list.{UserContribs, _}
import org.scalawiki.dto.cmd.query.meta.{EditCount, GuiUser, _}
import org.scalawiki.dto.cmd.query.prop.iiprop.{IiProp, Timestamp}
import org.scalawiki.dto.cmd.query.prop.rvprop.RvProp
import org.scalawiki.dto.cmd.query.prop.{InProp, Revisions, SubjectId, _}
import org.scalawiki.dto.cmd.query.{Generator, Query, TitlesParam}
import org.scalawiki.dto.{Contributor, Page}
import org.scalawiki.time.TimeRange

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait QueryLibrary {

  def imagesByGenerator(generator: Generator, withUrl: Boolean = false): Action = {
    import org.scalawiki.dto.cmd.query.prop._

    val iiProps = Seq(Timestamp, iiprop.User, iiprop.Size) ++ (if (withUrl) Seq(iiprop.Url) else Seq.empty)

    Action(Query(
      Prop(
        Info(),
        Revisions(RvProp(rvprop.Ids, rvprop.Content, rvprop.Timestamp, rvprop.User, rvprop.UserId)),
        ImageInfo(IiProp(iiProps:_*))
      ),
      generator
    ))
  }

  val allUsersQuery =
    Action(Query(ListParam(
      AllUsers(
        AuProp(Seq("registration", "editcount", "blockinfo")),
        AuWithEditsOnly(true), AuLimit("max"), AuExcludeGroup(Seq("bot")))
    )))

  val activeUsersQuery =
    Action(Query(ListParam(
      AllUsers(
        AuActiveUsers(true),
        AuProp(Seq("registration", "editcount", "blockinfo")),
        AuWithEditsOnly(true), AuLimit("max"), AuExcludeGroup(Seq("bot")))
    )))

  def userContribs(username: String, range: TimeRange, limit: String = "max", dir: String = "older"): Action = {
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
      GuiProp(Merged, Unattached, EditCount),
      GuiUser(username)
    ))))

  def whatLinksHere(title: String, ns: Int) = Action(Query(
    Prop(Links(PlNamespace(Seq(ns)), PlLimit("max"))),
    TitlesParam(Seq(title))
  ))

  def generatorWithTemplate(template: String, ns: Set[Int] = Set.empty): Generator = {
    val params = Seq(
      EiTitle("Template:" + template),
      EiLimit("500")
    ) ++ (if (ns.nonEmpty) Seq(EiNamespace(ns.toSeq)) else Seq.empty)

    Generator(EmbeddedIn(params: _*))
  }

  def categoryMembersGenerator(category: String, ns: Set[Int] = Set.empty) =
    Generator(CategoryMembers(
      (Seq(CmTitle("Category:" + category))
        ++ (if (ns.nonEmpty) Seq(CmNamespace(ns.toSeq)) else Seq.empty)): _*
    ))

  def pagesWithTemplate(template: String): Action =
    pagesByGenerator(generatorWithTemplate(template))

  def pagesByGenerator(generator: Generator): Action = {
    Action(Query(
      Prop(
        Info(InProp(SubjectId)),
        Revisions()
      ),
      generator))
  }

  def articlesWithTemplate(template: String)(implicit bot: MwBot): Future[Seq[Long]] = {
    bot.run(pagesWithTemplate(template)).map {
      pages =>
        pages.map(p => p.subjectId.getOrElse(p.id.get))
    }
  }

  def pagesToUsers(pages: Seq[Page]): Seq[Contributor] = pages.flatMap(_.lastRevisionUser)

  def getUsers(action: Action)(implicit bot: MwBot): Future[Seq[Contributor]] =
    bot.run(action).map(pagesToUsers)
}
