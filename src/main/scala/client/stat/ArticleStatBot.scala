package client.stat

import client.dto.cmd.ActionParam
import client.dto.cmd.query.list.{EiLimit, EiTitle, EmbeddedIn}
import client.dto.cmd.query.prop._
import client.dto.cmd.query.{Generator, PageIdsParam, Query}
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
          RvProp(Ids, Size, User, UserId, Timestamp),
          RvLimit("max")
        )
      )
    ))

    new DslQuery(action, bot).run.map(_.headOption)
  }

  def stat() = {

    val from = Some(DateTime.parse("2015-01-06T00:00+02:00"))
    val to = Some(DateTime.parse("2015-01-28T00:00+02:00"))
    for (newPagesIds <- pagesWithTemplate(ArticleStatBot.newTemplate);
         improvedPagesIds <- pagesWithTemplate(ArticleStatBot.improvedTemplate)) {
      println(s"New ${newPagesIds.size} $newPagesIds")
      println(s"Improved ${improvedPagesIds.size} $improvedPagesIds")

      for (withRevsNew <- pagesRevisions(newPagesIds);
           withRevsImproved <- pagesRevisions(improvedPagesIds)) {
        val all = withRevsNew ++ withRevsImproved


        val (created, improved) = all.partition(_.history.createdAfter(from))

        val allStat = new ArticleStat(from, to, all, "All")
        val createdStat = new ArticleStat(from, to, created, "created")
        val improvedStat = new ArticleStat(from, to, improved, "improved")

        println(Seq(allStat, createdStat, improvedStat).mkString("\n"))
      }
    }
  }
}

class ArticleStat(val from: Option[DateTime], val to: Option[DateTime], val pages: Seq[Page], label: String) {

  val users = pages.flatMap(_.history.users())

  val pagesWithDeltas:Seq[(Page, Int)] = pages.map(p => p -> p.history.delta(from, to).getOrElse(0))

  val deltas = pagesWithDeltas.map(_._2)

  val sorted = pagesWithDeltas.sortBy(_._2)

  val added = deltas.sum
  val maxAdded = deltas.max
  val minAdded = deltas.min
  val average = added / deltas.size

  def titlesAndNumbers(seq:Seq[(Page, Int)]) = seq.map{case (page, size) => s"[[${page.title}]] ($size)"}.mkString(", ")

  override def toString = {
    val top: Int = Math.min(20, sorted.size)
    val bottom: Int = Math.min(50, sorted.size)
    s"""
       |=== $label articles ===
                    |* Number of articles: ${pages.size}
        |* Authors: ${users.size}
        |====  Characters ====
        |* Added: $added
        |* Max added: ${titlesAndNumbers(sorted.takeRight(top).reverse)}
        |* Min added:  ${titlesAndNumbers(sorted.take(bottom))}
        |* Average added: $average
        |""".stripMargin
  }

}

object ArticleStatBot {

  val newTemplate = "Kherson-week-new"

  val improvedTemplate = "Kherson-week-improve"

  def main(args: Array[String]) {
    new ArticleStatBot().stat()
  }
}