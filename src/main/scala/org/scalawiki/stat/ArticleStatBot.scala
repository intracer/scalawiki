package org.scalawiki.stat

import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.list.{EiLimit, EiTitle, EmbeddedIn}
import org.scalawiki.dto.cmd.query.prop._
import org.scalawiki.dto.cmd.query.{Generator, PageIdsParam, Query}
import org.scalawiki.query.DslQuery
import org.scalawiki.{MwBot, WithBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ArticleStatBot(val event: ArticlesEvent) extends WithBot {

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

    new DslQuery(action, bot).run().map {
      pages =>
        pages.map(p => p.subjectId.getOrElse(p.id.get))
    }
  }

  def pagesRevisions(ids: Seq[Long]): Future[Seq[Page]] = {
    Future.traverse(ids)(pageRevisions).map(_.flatten)
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

    new DslQuery(action, bot).run().map { pages =>
      pages.headOption
    }
  }

  def stat() = {

    val from = Some(event.start)
    val to = Some(event.end)

    for (newPagesIds <- pagesWithTemplate(event.newTemplate);
         improvedPagesIds <- pagesWithTemplate(event.improvedTemplate)) {
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

object ArticleStatBot {

  def main(args: Array[String]) {
    new ArticleStatBot(Events.WLEWeek).stat()
  }
}