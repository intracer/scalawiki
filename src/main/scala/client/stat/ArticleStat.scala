package client.stat

import client.dto.cmd.ActionParam
import client.dto.cmd.query.list.{EiLimit, EiTitle, EmbeddedIn}
import client.dto.cmd.query.prop._
import client.dto.cmd.query.{PageIdsParam, Generator, Query}
import client.dto.{DslQuery, Page}
import client.{MwBot, WithBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ArticleStat extends WithBot {

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

  def pageRevisions(ids: Seq[Page.Id]): Future[Seq[Page]] = {
    val action = ActionParam(Query(
      PageIdsParam(ids),
      PropParam(
        Info(),
        Revisions()
      )
    ))

    new DslQuery(action, bot).run.map {
      pages =>
        pages
    }
  }


  def stat() = {
    for (newPagesIds <- pagesWithTemplate(ArticleStat.newTemplate)
        //; improvedPagesIds <- pagesWithTemplate(ArticleStat.improvedTemplate)
    ) {
      println(s"New ${newPagesIds.size} $newPagesIds")
      //println(s"Improved ${improvedPagesIds.size} $improvedPagesIds")

      for (revs <- pageRevisions(newPagesIds)) {
        println(s"New ${revs.size} $revs")
      }
    }
  }
}

object ArticleStat {

  val newTemplate = "Kherson-week-new"

  val improvedTemplate = "Kherson-week-improve"

  def main(args: Array[String]) {
    new ArticleStat().stat()
  }
}