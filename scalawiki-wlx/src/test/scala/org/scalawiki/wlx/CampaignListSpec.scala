package org.scalawiki.wlx

import org.scalawiki.{HasBot, MwBot}
import org.scalawiki.dto.{Namespace, Page}
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.util.{HttpStub, MockBotSpec}
import org.scalawiki.util.TestUtils.resourceAsString
import org.scalawiki.wlx.dto.{ContestType, HasImagesCategory}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import spray.util.pimpFuture

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CampaignListSpec extends Specification with MockBotSpec with Mockito {

  implicit def titleToHasImages(title: String): HasImagesCategory = new HasImagesCategory {
    override def imagesCategory = title
  }

  def campaignList(testBot: MwBot) = new CampaignList() with HasBot {
    val bot = testBot
  }

  def categoryQueryBot(title: String, responseFile: String) = {
    val response = resourceAsString(responseFile)
    val cmd = HttpStub(Map("action" -> "query", "list" -> "categorymembers",
      "cmtitle" -> title,
      "cmnamespace" -> Namespace.CATEGORY.toString,
      "continue" -> "", "cmlimit" -> "max"),
      response)
    getBot(cmd)
  }

  def mockedBot(title: String, categories: Seq[String]) = {
    val bot = mock[MwBot]

    val action = Action(Query(ListParam(CategoryMembers(
      CmTitle(title), CmNamespace(Seq(Namespace.CATEGORY)), CmLimit("max"))
    )))

    bot.run(action) returns Future {
      categories.map(cat => Page(title = cat))
    }

    bot
  }

  "CampaignList" should {
    "return WLM years" in {
      val bot = categoryQueryBot("Category:Images from Wiki Loves Monuments", "/org/scalawiki/wlx/WLM_years.json")

      val result = campaignList(bot).getContests(ContestType.WLM).await

      result must have size 8
      result.map(_.year) === (2010 to 2017)
      result.map(_.contestType).distinct === Seq(ContestType.WLM)
    }

    "return WLE years" in {
      val bot = categoryQueryBot("Category:Images from Wiki Loves Earth", "/org/scalawiki/wlx/WLE_years.json")

      val result = campaignList(bot).getContests(ContestType.WLE).await
      val byYear = result.sortBy(_.year)

      byYear must have size 5
      byYear.map(_.year) === (2013 to 2017)
      byYear.map(_.contestType).distinct === Seq(ContestType.WLE)
    }
  }

  "return WLM 2011" in {
    val title = "Category:Images from Wiki Loves Monuments 2011"
    val categories = Seq(
      "Category:Images from Wiki Loves Monuments 2011 in Andorra",
      "Category:Images from Wiki Loves Monuments 2011 in Hungary",
      "Category:Images from Wiki Loves Monuments 2011 in Hungary - international",
      "Category:Images from Wiki Loves Monuments 2011 in the Netherlands",
      "Category:Images from Wiki Loves Monuments 2011 with something wrong",
      "Category:Featured pictures from Wiki Loves Monuments 2011",
      "Category:Valued images from Wiki Loves Monuments 2011",
      "Category:Art nouveau images from Wiki Loves Monuments 2011",
      "Category:Categories containing possible art nouveau images for Wiki Loves Monuments 2011",
      "Category:Videos from Wiki Loves Monuments 2011",
      "Category:Quality images from Wiki Loves Monuments 2011"
    )

    val bot = mockedBot(title, categories)

    val result = campaignList(bot).getContests(title).await

    result.map(_.year).distinct === Seq(2011)
    result.map(_.contestType).distinct === Seq(ContestType.WLM)
    result.map(_.country.name) === Seq("Andorra", "Hungary", "Hungary - international", "the Netherlands")
  }

  "return WLM 2012" in {
    val title = "Category:Images from Wiki Loves Monuments 2012"
    val categories = Seq(
      "Category:Images from Wiki Loves Monuments 2012 in the Czech Republic",
      "Category:Images from Wiki Loves Monuments 2012 in the Philippines",
      "Category:Images from Wiki Loves Monuments 2012 in South Africa",
      "Category:Images from Wiki Loves Monuments 2012 in Ukraine",
      "Category:Images from Wiki Loves Monuments 2012 in the United States",
      "Category:Images from Wiki Loves Monuments 2012 in an unknown country",
      "Category:Images from Wiki Loves Monuments 2012 with a problem",
      "Category:Featured pictures from Wiki Loves Monuments 2012",
      "Category:Valued images from Wiki Loves Monuments 2012",
      "Category:GLAM images from Wiki Loves Monuments 2012",
      "Category:Nominated pictures for Wiki Loves Monuments International 2012",
      "Category:Quality images from Wiki Loves Monuments 2012",
      "Category:Images from Wiki Loves Monuments 2012 used in valued image sets"
    )

    val bot = mockedBot(title, categories)

    val result = campaignList(bot).getContests(title).await

    result.map(_.year).distinct === Seq(2012)
    result.map(_.contestType).distinct === Seq(ContestType.WLM)
    result.map(_.country.name) === Seq("the Czech Republic", "the Philippines", "South Africa", "Ukraine", "the United States", "an unknown country")
  }

  "return WLE 2014" in {
    val title = "Category:Images from Wiki Loves Earth 2014"
    val categories = Seq(
      "Category:Featured pictures from Wiki Loves Earth 2014",
      "Category:Valued images from Wiki Loves Earth 2014",
      "Category:Quality images from Wiki Loves Earth 2014",
      "Category:Images from Wiki Loves Earth 2014 in Algeria",
      "Category:Images from Wiki Loves Earth 2014 in Andorra & Catalan areas",
      "Category:Images from Wiki Loves Earth 2014 in Armenia & Nagorno-Karabakh",
      "Category:Images from Wiki Loves Earth 2014 in the Netherlands"
    )

    val bot = mockedBot(title, categories)

    val result = campaignList(bot).getContests(title).await

    result.map(_.year).distinct === Seq(2014)
    result.map(_.contestType).distinct === Seq(ContestType.WLE)
    result.map(_.country.name) === Seq("Algeria", "Andorra & Catalan areas", "Armenia & Nagorno-Karabakh", "the Netherlands")
  }

  "return WLE 2016" in {
    val title = "Category:Images from Wiki Loves Earth 2016"
    val categories = Seq(
      "Category:Featured pictures from Wiki Loves Earth 2016",
      "Category:Quality images from Wiki Loves Earth 2016",
      "Category:Valued images from Wiki Loves Earth 2016",
      "Category:Images from Wiki Loves Earth 2016 in Albania",
      "Category:Images from Wiki Loves Earth Biosphere Reserves 2016"
    )

    val bot = mockedBot(title, categories)

    val result = campaignList(bot).getContests(title).await

    result.map(_.year).distinct === Seq(2016)
    result.map(_.contestType).distinct === Seq(ContestType.WLE)
    result.map(_.country.name) === Seq("Albania"/*, "Biosphere Reserves 2016"*/)
  }
}