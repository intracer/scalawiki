package org.scalawiki.wlx

import org.scalawiki.dto.Namespace
import org.scalawiki.util.HttpStub
import org.scalawiki.util.TestUtils.resourceAsString
import org.scalawiki.wlx.dto.ContestType
import org.specs2.mutable.Specification
import spray.util.pimpFuture

class CampaignListSpec extends Specification {

  val wlmCats = Map(
    "Category:Images from Wiki Loves Monuments 2010" ->
      Seq(
        "Category:Featured pictures from Wiki Loves Monuments 2010",
        "Category:Valued images from Wiki Loves Monuments 2010",
        "Category:Quality images from Wiki Loves Monuments 2010"
      ),

    "Category:Images from Wiki Loves Monuments 2011" ->
      Seq(
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
      ),

    "Category:Images from Wiki Loves Monuments 2012" ->
      Seq(
        "Category:Images from Wiki Loves Monuments 2012 in Andorra",
        "Category:Images from Wiki Loves Monuments 2012 in the Czech Republic",
        "Category:Images from Wiki Loves Monuments 2012 in the Netherlands",
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
      ),

    "Category:Images from Wiki Loves Monuments 2013" ->
      Seq(
        "Category:Images from Wiki Loves Monuments 2013 in Algeria",
        "Category:Images from Wiki Loves Monuments 2013 in Antarctica",
        "Category:Images from Wiki Loves Monuments 2013 in the Czech Republic",
        "Category:Images from Wiki Loves Monuments 2013 in El Salvador",
        "Category:Images from Wiki Loves Monuments 2013 in Hong Kong",
        "Category:Images from Wiki Loves Monuments 2013 in the Netherlands",
        "Category:Images from Wiki Loves Monuments 2013 in the Philippines",
        "Category:Images from Wiki Loves Monuments 2013 in South Africa",
        "Category:Images from Wiki Loves Monuments 2013 in the United Kingdom",
        "Category:Images from Wiki Loves Monuments 2013 in the United States",
        "Category:Images from Wiki Loves Monuments 2013 in an unknown country",
        "Category:Images from Wiki Loves Monuments 2013 with a problem",
        "Category:Featured pictures from Wiki Loves Monuments 2013",
        "Category:Valued images from Wiki Loves Monuments 2013",
        "Category:GLAM images from Wiki Loves Monuments 2013",
        "Category:Nominated pictures for Wiki Loves Monuments International 2013",
        "Category:Quality images from Wiki Loves Monuments 2013",
        "Category:WWI related images from Wiki Loves Monuments 2013"
      ),

    "Category:Images from Wiki Loves Monuments 2014" ->
      Seq(
        "Category:Images from Wiki Loves Monuments 2014 in Albania",
        "Category:Images from Wiki Loves Monuments 2014 in the Czech Republic",
        "Category:Images from Wiki Loves Monuments 2014 in Hong Kong",
        "Category:Images from Wiki Loves Monuments 2014 in Kosovo",
        "Category:Images from Wiki Loves Monuments 2014 in Palestine",
        "Category:Images from Wiki Loves Monuments 2014 in South Africa",
        "Category:Images from Wiki Loves Monuments 2014 in the United Kingdom",
        "Category:Images from Wiki Loves Monuments 2014 in the United States",
        "Category:Images from Wiki Loves Monuments 2014 in an unknown country",
        "Category:Images from Wiki Loves Monuments 2014 with a problem",
        "Category:Featured pictures from Wiki Loves Monuments 2014",
        "Category:Valued images from Wiki Loves Monuments 2014",
        "Category:Quality images from Wiki Loves Monuments 2014"
      ),

    "Category:Images from Wiki Loves Monuments 2015" ->
      Seq(
        "Category:Images from Wiki Loves Monuments 2015 in Albania",
        "Category:Images from Wiki Loves Monuments 2015 in Armenia & Nagorno-Karabakh",
        "Category:Images from Wiki Loves Monuments 2015 in Kosovo",
        "Category:Images from Wiki Loves Monuments 2015 in Macedonia",
        "Category:Images from Wiki Loves Monuments 2015 in the Netherlands",
        "Category:Images from Wiki Loves Monuments 2015 in South Africa",
        "Category:Featured pictures from Wiki Loves Monuments 2015",
        "Category:Valued images from Wiki Loves Monuments 2015",
        "Category:Images from Wiki Loves Monuments 2015 in South Tyrol",
        "Category:Quality images from Wiki Loves Monuments 2015"
      )
  )

  val wleCats = Map(
    "Category:Images from Wiki Loves Earth 2014" ->
      Seq(
        "Category:Featured pictures from Wiki Loves Earth 2014",
        "Category:Valued images from Wiki Loves Earth 2014",
        "Category:Quality images from Wiki Loves Earth 2014",
        "Category:Images from Wiki Loves Earth 2014 in Algeria",
        "Category:Images from Wiki Loves Earth 2014 in Andorra & Catalan areas",
        "Category:Images from Wiki Loves Earth 2014 in Armenia & Nagorno-Karabakh",
        "Category:Images from Wiki Loves Earth 2014 in the Netherlands"
      ),

    "Category:Images from Wiki Loves Earth 2015" ->
      Seq(
        "Category:Featured pictures from Wiki Loves Earth 2015",
        "Category:Quality images from Wiki Loves Earth 2015",
        "Category:Valued images from Wiki Loves Earth 2015",
        "Category:Images from Wiki Loves Earth 2015 in an unknown country",
        "Category:Images from Wiki Loves Earth 2015 in Algeria",
        "Category:Images from Wiki Loves Earth 2015 in Andorra & Catalan areas",
        "Category:Images from Wiki Loves Earth 2015 in South Tyrol"
      ),

    "Category:Images from Wiki Loves Earth 2016" ->
      Seq(
        "Category:Featured pictures from Wiki Loves Earth 2016",
        "Category:Quality images from Wiki Loves Earth 2016",
        "Category:Valued images from Wiki Loves Earth 2016",
        "Category:Images from Wiki Loves Earth 2016 in Albania",
        "Category:Images from Wiki Loves Earth Biosphere Reserves 2016"
      ),

    "Category:Images from Wiki Loves Earth 2017" ->
      Seq(
        "Category:Images from Wiki Loves Earth 2017 in Australia",
        "Category:Images from Wiki Loves Earth 2017 in South Korea"
      )
  )

  val contests = Map(
    "Category:Images from Wiki Loves Monuments" -> wlmCats,
    "Category:Images from Wiki Loves Earth" -> wleCats
  )

  "CampaignList" should {
    "return WLM years" in {

      val response = resourceAsString("/org/scalawiki/wlx/WLM_years.json")

      val commands = Seq(
        new HttpStub(Map("action" -> "query", "list" -> "categorymembers",
          "cmtitle" -> "Category:Images from Wiki Loves Monuments",
          "cmnamespace" -> Namespace.CATEGORY.toString,
          "continue" -> ""), response)
      )

      val result = CampaignList.getContests(ContestType.WLM).await

      result must have size 8
      result.map(_.year) === (2010 to 2017)
      result.map(_.contestType).distinct === Seq(ContestType.WLM)
    }

    "return WLE years" in {

      val response = resourceAsString("/org/scalawiki/wlx/WLE_years.json")

      val commands = Seq(
        new HttpStub(Map("action" -> "query", "list" -> "categorymembers",
          "cmtitle" -> "Category:Images from Wiki Loves Earth",
          "cmnamespace" -> Namespace.CATEGORY.toString,
          "continue" -> ""), response)
      )

      val result = CampaignList.getContests(ContestType.WLE).await
      val byYear = result.sortBy(_.year)

      byYear must have size 5
      byYear.map(_.year) === (2013 to 2017)
      byYear.map(_.contestType).distinct === Seq(ContestType.WLE)
    }
  }
}