package org.scalawiki.stat

import org.joda.time.DateTime
import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.list.{EiLimit, EiTitle, EmbeddedIn}
import org.scalawiki.dto.cmd.query.prop._
import org.scalawiki.dto.cmd.query.{Generator, PageIdsParam, Query}
import org.scalawiki.query.DslQuery
import org.scalawiki.{MwBot, WithBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ArticleStatBot extends WithBot {

  val host = MwBot.ukWiki

  def pagesWithTemplate(template: String): Future[Seq[Page.Id]] = {
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
        pages.map(p => p.subjectId.getOrElse(p.id))
    }
  }

  def pagesRevisions(ids: Seq[Page.Id]): Future[Seq[Page]] = {
    Future.traverse(ids)(pageRevisions).map(_.flatten)
  }

  def pageRevisions(id: Page.Id): Future[Option[Page]] = {
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

    val from = Some(DateTime.parse("2015-02-12T00:00+02:00"))
    val to = Some(DateTime.parse("2015-02-27T00:00+02:00"))

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








object ArticleStatBot {

  val newTemplate = "Zaporizhia2-week-new"
    //"Kherson-week-new"

  val improvedTemplate = "Zaporizhia2-week-improve"
    //"Kherson-week-improve"

  def main(args: Array[String]) {
    new ArticleStatBot().stat()
  }
}