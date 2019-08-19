package org.scalawiki.wlx

import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.specs2.mutable.Specification

class UnknownPlacesSpec extends Specification {

  val campaign = "wlm-ua"
  val contest = Contest.byCampaign(campaign).get.copy(year = 2019)
  val db = new MonumentDB(contest, Nil)
  val headers = Seq("region Id", "name", "candidates", "monuments")

  "report nothing" in {
    db.reportUnknownPlaces(Nil) === Nil
  }

  "report one no candidates" in {
    val monument = new Monument("page1", "regionId1-1", "monument1")
    val place = UnknownPlace("page1", "regionId1", "place1", Nil, Seq(monument))

    db.reportUnknownPlaces(Seq(place)) === Seq(Table(headers, Seq(
      Seq("regionId1", "place1", "", "monument1")
    ), "page1"))
  }

  "report group by page" in {
    val monument1 = new Monument("page1", "regionId1-1", "monument1")
    val monument2 = new Monument("page1", "regionId1-2", "monument2")
    val monument3 = new Monument("page2", "regionId2-3", "monument3")
    val monument4 = new Monument("page2", "regionId3-4", "monument4")
    val places = Seq(
      UnknownPlace("page1", "regionId1", "place1", Nil, Seq(monument1)),
      UnknownPlace("page1", "regionId1", "place2", Nil, Seq(monument2)),
      UnknownPlace("page2", "regionId2", "place3", Nil, Seq(monument3)),
      UnknownPlace("page2", "regionId3", "place4", Nil, Seq(monument4))
    )

    db.reportUnknownPlaces(places) === Seq(
      Table(headers, Seq(
        Seq("regionId1", "place1", "", "monument1"),
        Seq("regionId1", "place2", "", "monument2")
      ), "page1"),
      Table(headers, Seq(
        Seq("regionId2", "place3", "", "monument3"),
        Seq("regionId3", "place4", "", "monument4")
      ), "page2")
    )
  }
}