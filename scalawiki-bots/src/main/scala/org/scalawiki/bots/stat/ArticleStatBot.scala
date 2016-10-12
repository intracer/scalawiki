package org.scalawiki.bots.stat

import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.list.{EiLimit, EiTitle, EmbeddedIn}
import org.scalawiki.dto.cmd.query.prop._
import org.scalawiki.dto.cmd.query.{Generator, PageIdsParam, Query}
import org.scalawiki.dto.filter.RevisionFilterDateAndUser
import org.scalawiki.{MwBot, WithBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ArticleStatBot() extends WithBot {

  val host = MwBot.ukWiki

  def pagesWithTemplate(template: String): Future[Seq[Long]] = {
    val action = Action(Query(
      Prop(
        Info(InProp(SubjectId)),
        Revisions()
      ),
      Generator(EmbeddedIn(
        EiTitle("Template:" + template),
        EiLimit("500")
      ))
    ))

    bot.run(action).map {
      pages =>
        pages.map(p => p.subjectId.getOrElse(p.id.get))
    }
  }

  def pagesRevisions(ids: Seq[Long]): Future[TraversableOnce[Option[Page]]] = {
    Future.traverse(ids)(pageRevisions)
  }

  def pageRevisions(id: Long): Future[Option[Page]] = {
    import org.scalawiki.dto.cmd.query.prop.rvprop._

    val action = Action(Query(
      PageIdsParam(Seq(id)),
      Prop(
        Info(),
        Revisions(
          RvProp(Content, Ids, Size, User, UserId, Timestamp),
          RvLimit("max")
        )
      )
    ))

    bot.run(action).map { pages =>
      pages.headOption
    }
  }

  def stat(event: ArticlesEvent): Future[EventStat] = {

    val from = Some(event.start)
    val to = Some(event.end)

    val revisionFilter = new RevisionFilterDateAndUser(from, to)

    val newPagesIdsF = pagesWithTemplate(event.newTemplate)
    val improvedPagesIdsF = pagesWithTemplate(event.improvedTemplate)

    Future.sequence(Seq(newPagesIdsF, improvedPagesIdsF)).flatMap {
      ids =>

        val newPagesIds = ids.head
        val improvedPagesIds = ids.last
        println(s"New ${newPagesIds.size} $newPagesIds")
        println(s"Improved ${improvedPagesIds.size} $improvedPagesIds")

        val allIds = newPagesIds.toSet ++ improvedPagesIds.toSet

        pagesRevisions(allIds.toSeq).map { allPages =>

          val revStats = allPages.map {
            case Some(page)
              if page.history.editedIn(revisionFilter) =>
              Some(RevisionStat.fromPage(page, revisionFilter))
            case _ => None
          }.flatten.toSeq.sortBy(-_.addedOrRewritten)

          val stat = new EventStat(event, revStats)

          println(Seq(stat.allStat, stat.createdStat, stat.improvedStat).mkString("\n"))

          stat
        }
    }
  }
}

case class EventStat(event: ArticlesEvent, revStats: Seq[RevisionStat]) {

  val from = Some(event.start)
  val to = Some(event.end)

  val revisionFilter = new RevisionFilterDateAndUser(from, to)

  val (created, improved) = revStats.partition(_.history.createdAfter(from))

  val allStat = new ArticleStat(revisionFilter, revStats, "All")
  val createdStat = new ArticleStat(revisionFilter, created, "created")
  val improvedStat = new ArticleStat(revisionFilter, improved, "improved")

}

object ArticleStatBot {

  def main(args: Array[String]) {
    val bot = new ArticleStatBot()

    val (contests, weeks) = Events.events()

    weeks.find(_.newTemplate == "Cherkasy-week-new").map(bot.stat)

//    Future.sequence(weeks.map(bot.stat)).map(eventSummary)
//    Future.sequence(contests.map(bot.stat)).map(eventSummary)
  }

  def eventSummary(stats: Seq[Long]): Unit = {
    val events = stats.size
    val added = stats.sum
    println(s"!!!!!!!!! weeks: $events, bytes: $added")
    println(s"!!!!!!!!! weeks: $stats")
  }
}