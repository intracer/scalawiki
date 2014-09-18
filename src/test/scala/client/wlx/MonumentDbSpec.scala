package client.wlx

import client.wlx.dto.{Contest, Monument, Region}
import org.specs2.mutable.Specification

class MonumentDbSpec extends Specification {

  val monuments = Region.Ukraine.keySet.flatMap{
    regionId =>
      (1 to regionId.toInt).map { i =>
        Monument(
          textParam = "",
          pageParam = "",
          id = regionId + "-001-" + f"$i%04d",
          name = "Monument in " + Region.Ukraine(regionId)
        )
      }
  }

   "monument db" should {
     "contain monuments ids" in {
       val contest = Contest.WLMUkraine(2014, "09-15", "10-15")

       val db = new MonumentDB(contest, monuments.toSeq)

       db.ids.size === monuments.size
       db.ids ===  monuments.map(_.id)
     }

    "group monuments by regions" in {
      val contest = Contest.WLMUkraine(2014, "09-15", "10-15")

      val db = new MonumentDB(contest, monuments.toSeq)

      val regions = db._byRegion.keySet

      for (region <- regions) yield {
        db._byRegion(region).size === region.toInt
      }

      regions === Region.Ukraine.keySet
    }
  }

}
