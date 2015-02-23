package client.stat

import client.dto.cmd.ActionParam
import client.dto.cmd.query.list.{EiLimit, EiTitle, EmbeddedIn}
import client.dto.cmd.query.prop._
import client.dto.cmd.query.{Generator, PageIdsParam, Query}
import client.dto.history.{RevisionFilter, RevisionStat}
import client.dto.{DslQuery, Page}
import client.{MwBot, WithBot}
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ArticleStatBot extends WithBot {

  val host = MwBot.ukWiki

  def pagesWithTemplate(template: String): Future[Seq[Page.Id]] = {
    val action = ActionParam(Query(
      PropParam(
        Info(InProp(SubjectId)),
        Revisions()
      ),
      Generator(EmbeddedIn(
        EiTitle("Template:" + template),
        EiLimit("500")
      ))
    ))

    new DslQuery(action, bot).run.map {
      pages =>
        pages.map(p => p.subjectId.getOrElse(p.id))
    }
  }

  def pagesRevisions(ids: Seq[Page.Id]): Future[Seq[Page]] = {
    Future.traverse(ids)(pageRevisions).map(_.flatten)
  }

  def pageRevisions(id: Page.Id): Future[Option[Page]] = {
    val action = ActionParam(Query(
      PageIdsParam(Seq(id)),
      PropParam(
        Info(),
        Revisions(
          RvProp(Content, Ids, Size, User, UserId, Timestamp),
          RvLimit("max")
        )
      )
    ))

    new DslQuery(action, bot).run.map { pages =>
      pages.headOption
    }
  }

  def stat() = {

    val from = Some(DateTime.parse("2015-01-21T00:00+02:00"))
    val to = Some(DateTime.parse("2015-02-12T00:00+02:00"))

//    val from = Some(DateTime.parse("2015-01-06T00:00+02:00"))
//    val to = Some(DateTime.parse("2015-01-28T00:00+02:00"))
    for (newPagesIds <- pagesWithTemplate(ArticleStatBot.newTemplate);
         improvedPagesIds <- pagesWithTemplate(ArticleStatBot.improvedTemplate)) {
      println(s"New ${newPagesIds.size} $newPagesIds")
      println(s"Improved ${improvedPagesIds.size} $improvedPagesIds")

      val allIds = newPagesIds.toSet ++ improvedPagesIds.toSet

      for (allPages <- pagesRevisions(allIds.toSeq)) {

        val updatedPages = allPages.filter(_.history.editedIn(from, to))
        val (created, improved) = updatedPages.partition(_.history.createdAfter(from))

        val allStat = new ArticleStat(from, to, updatedPages, "All")
        val createdStat = new ArticleStat(from, to, created, "created")
        val improvedStat = new ArticleStat(from, to, improved, "improved")

        println(Seq(allStat, createdStat, improvedStat).mkString("\n"))
      }
    }
  }
}

class ArticleStat(val from: Option[DateTime], val to: Option[DateTime], val pages: Seq[Page], label: String) {

  val filter = new RevisionFilter(from, to)
  val revisionStats = pages.map(page => new RevisionStat(page, filter)).sortBy(-_.addedOrRewritten)

  val deltas = revisionStats.map(_.delta)
  val addedOrRewritten = revisionStats.map(_.addedOrRewritten)

  val added = new NumericArrayStat("Added", deltas)
  val addedOrRewrittenStat = new NumericArrayStat("Added or rewritten", addedOrRewritten)

  val userStat = new UserStat(revisionStats)

  //  def titlesAndNumbers(seq: Seq[(Page, Int)]) = seq.map { case (page, size) => s"[[${page.title}]] ($size)"}.mkString(", ")

  def pageStat = {
    val header = "{| class='wikitable sortable'\n" +
      "|+ pages\n" + RevisionStat.statHeader

    header + revisionStats.map(_.stat).mkString("\n|-\n", "\n|-\n", "") + "\n|}"
  }

  override def toString = {
    s"""
        |=== $label articles ===
        |* Number of articles: ${pages.size}
        |* Authors: ${userStat.users.size}
        |====  Bytes ====
        |* $addedOrRewrittenStat
        |* $added
        |""".stripMargin +
      "\n====  Page stat ====\n" + pageStat +
      "\n====  User stat ====\n" + userStat

  }
}

class UserStat(revisionStats: Seq[RevisionStat]) {
  val users = revisionStats.foldLeft(Set.empty[String]) {
    (users, stats) => users ++ stats.users
  }

  val byUser = users.map { user =>
    (user, revisionStats.filter(_.users.contains(user)).sortBy(-_.byUserSize(user)))
  }.toMap

  val byUserAddedOrRemoved = byUser.toSeq.map {
    case (user, statSeq) => (user, statSeq.map(_.byUserSize(user)).sum)
  }.sortBy{
    case (user, size) => -size
  }

  override def toString = {
    val header = "{| class='wikitable sortable'\n" +
      "|+ users\n" + "! user !! added or rewritten !! articles number !! article list\n"

    header + byUserAddedOrRemoved.map{
      case (user, size) =>
        s"| [[User:$user|$user]] || $size || ${byUser(user).size} || ${byUser(user).map(rs => s"[[${rs.page.title}]] ${rs.byUserSize(user)}").mkString("<br>")}"
    }
      .mkString("\n|-\n", "\n|-\n", "")  + "\n|}"
  }


}


class NumericArrayStat(val name: String, val data: Seq[Int]) {
  val sum = data.sum
  val max = if (data.isEmpty) 0 else data.max
  val min = if (data.isEmpty) 0 else  data.min
  val average = if (data.isEmpty) 0 else sum / data.size

  override def toString = s"$name $sum (max: $max, min: $min, average: $average)"
}

object ArticleStatBot {

  val newTemplate = "Architecture-week-new"
    //"Kherson-week-new"

  val improvedTemplate = "Architecture-week-improve"
    //"Kherson-week-improve"

  def main(args: Array[String]) {
    new ArticleStatBot().stat()
  }
}