package client.wlx.query

import client.wlx.dto.{Contest, Monument}
import client.{LoginInfo, MwBot}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

trait MonumentQuery {
  def listsAsync(template: String): Future[Seq[Monument]]
  def lists(template: String): Seq[Monument]
}

class MonumentQueryApi(contest: Contest) extends MonumentQuery {

  var bot: MwBot = _

  def initBot() = {
    bot = MwBot.create(contest.country.languageCode + "wikipedia.org")
    bot.await(bot.login(LoginInfo.login, LoginInfo.password))
  }

  override def listsAsync(template: String): Future[Seq[Monument]] = {
    bot.page("Template:" + template).revisionsByGenerator("embeddedin", "ei", Set.empty, Set("content", "timestamp", "user", "comment")) map {
      pages =>
        val monuments = pages.flatMap(page => Monument.monumentsFromText(page.text.getOrElse(""), page.title, template))
        monuments
    }
  }

  override def lists(template: String): Seq[Monument] = {
    bot.await(listsAsync(template))
  }


}


class MonumentQuerySeq(contest: Contest, monuments: Seq[Monument]) extends MonumentQuery {

  override def listsAsync(template: String): Future[Seq[Monument]]
  = future { lists(template) }

  override def lists(template: String): Seq[Monument] = monuments
}

