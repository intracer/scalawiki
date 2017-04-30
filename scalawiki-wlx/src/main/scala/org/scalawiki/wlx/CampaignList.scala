package org.scalawiki.wlx

import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.dto.{Namespace, Page}
import org.scalawiki.wlx.dto.{Contest, ContestType, HasImagesCategory}
import org.scalawiki.{HasBot, MwBot, WithBot}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CampaignList {
  self: HasBot =>

  def categoryMembers(parent: String): Future[Seq[Page]] = {
    bot.run(Action(Query(ListParam(CategoryMembers(
      CmTitle(parent), CmNamespace(Seq(Namespace.CATEGORY)), CmLimit("max"))
    ))))
  }

  def titles(pages: Seq[Seq[Page]]): Seq[String] = pages.flatten.map(_.title)

  def getContests(hasImages: HasImagesCategory): Future[Seq[Contest]] =
    contestsFromCategory(hasImages.imagesCategory)

  def contestsFromCategory(parent: String): Future[Seq[Contest]] = {
    for (cats <- categoryMembers(parent)) yield
      for (cat <- cats;
           contest <- CountryParser.fromCategoryName(cat.title)
      ) yield contest
  }

  def categoriesMembers(categories: Seq[String]): Future[Seq[Seq[Page]]] =
    Future.sequence(categories.map(categoryMembers))
}

object CampaignList extends CampaignList with WithBot {

  override def host = MwBot.commons

  def main(args: Array[String]): Unit = {
    val types = ContestType.all
    val contestCats = types.map(_.imagesCategory)

    for (yearCats <- categoriesMembers(contestCats)) {
      val imageCats = titles(yearCats).filter(CountryParser.isContestCategory)
      for (campaignCats <- categoriesMembers(imageCats)) {
        titles(campaignCats).foreach(println)
      }
    }
  }
}
