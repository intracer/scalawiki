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

}