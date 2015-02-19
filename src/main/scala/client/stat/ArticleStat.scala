package client.stat

import client.dto.{DslQuery, Page}
import client.dto.cmd.ActionParam
import client.dto.cmd.query.list.{EiLimit, EiTitle, EmbeddedIn}
import client.dto.cmd.query.{Generator, Query}
import client.dto.cmd.query.prop._
import client.{MwBot, WithBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ArticleStat extends WithBot {

  val host = MwBot.ukWiki

  def pagesWithTemplate(template: String): Future[Seq[Page]] = {
    val action = ActionParam(Query(
      PropParam(
        Info(InProp(SubjectId)),
        Revisions
      ),
      Generator(EmbeddedIn(
        EiTitle("Template:" + template),
        EiLimit("500")
      ))
    ))

    new DslQuery(action, bot).run
  }


  def stat() = {
    for (newPages <- pagesWithTemplate(ArticleStat.newTemplate);
         improvedPages <- pagesWithTemplate(ArticleStat.improvedTemplate)) {
      println(s"New ${newPages.size} $newPages")
      println(s"Improved ${improvedPages.size} $improvedPages")
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