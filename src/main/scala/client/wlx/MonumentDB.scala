package client.wlx

import client.{LoginInfo, MwBot}

import scala.concurrent.Await

class MonumentDB(val contest: Contest) {

  var bot: MwBot = _

  var monuments: Seq[Monument] = Seq.empty

  var _byId: Map[String, Seq[Monument]] = Map.empty

  def ids: Set[String] = _byId.keySet

  def initBot() = {
    bot = MwBot.create(contest.country.languageCode + "wikipedia.org")
    Await.result(bot.login(LoginInfo.login, LoginInfo.password), bot.http.timeout)
  }


  def fetchLists() = {
    monuments = bot.await(Monument.lists(bot, contest.listTemplate))
    _byId = monuments.groupBy(_.id)

  }

  def byId(id: String) = _byId.getOrElse(id, Seq.empty[Monument]).headOption

  def byRegion(regId: String) = monuments.filter(_.id.startsWith(regId))


}
