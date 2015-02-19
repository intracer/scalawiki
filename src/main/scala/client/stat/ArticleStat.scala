package client.stat

import client.dto.{DslQuery, Page}
import client.dto.cmd.ActionParam
import client.dto.cmd.query.list.{EiTitle, EmbeddedIn}
import client.dto.cmd.query.{Generator, Query}
import client.dto.cmd.query.prop._
import client.{MwBot, WithBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ArticleStat extends WithBot {

  val host = MwBot.ukWiki

  def pagesWithTemplate(template: String, ns: Set[Int] = Set.empty): Future[Seq[Page]] = {
    val query = bot.page("Template:" + template)

    query.revisionsByGenerator("embeddedin", "ei",
      ns, Set("content", "timestamp", "user", "userid", "comment", "ids"), None, "500")
  }

  def pagesWithTemplate2(template: String, ns: Set[Int] = Set.empty): Future[Seq[Page]] = {
    val action = ActionParam(Query(
      PropParam(
        Info(InProp(SubjectId)),
        Revisions
      ),
      Generator(EmbeddedIn(EiTitle("Template:Name")))
    ))

    new DslQuery(action, bot).run
  }

  def pagesWithTemplateNoTalk(template: String): Future[Seq[Page]] = {
    pagesWithTemplate(template).map {
      pagesAndTalks =>
        val (talks, pages) = pagesAndTalks.partition(_.isTalkPage)
        pages
    }
  }


  // Обговорення:


  def stat() = {
    for (newPages <- pagesWithTemplate(ArticleStat.newTemplate);
         improvedPages <- pagesWithTemplate(ArticleStat.improvedTemplate)) {
      println(s"New ${newPages.size} $newPages")
      println(s"New ${improvedPages.size} $improvedPages")
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