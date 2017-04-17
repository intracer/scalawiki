package org.scalawiki.wlx

import org.scalawiki.dto.Namespace
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.list.{CategoryMembers, CmNamespace, CmTitle, ListParam}
import org.scalawiki.wlx.dto.ContestType
import org.scalawiki.{MwBot, WithBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CampaignList extends WithBot {

  override def host = MwBot.commons

  def categoryMembers(parent: String): Action = {
    Action(Query(ListParam(CategoryMembers(
      CmTitle(parent), CmNamespace(Seq(Namespace.CATEGORY))
    ))))
  }

  def main(args: Array[String]): Unit = {
    val types = ContestType.all
    val contestCats = types.map(_.imagesCategory)
    val queries = contestCats.map(categoryMembers)

    for (yearCats <- Future.sequence(queries.map(bot.run(_)))) {
      val imageCats = yearCats.flatten.map(_.title).filter(CountryParser.isContestCategory)
      imageCats.sorted.foreach(println)
    }
  }

}
