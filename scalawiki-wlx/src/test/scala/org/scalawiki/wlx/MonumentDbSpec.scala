package org.scalawiki.wlx

import org.scalawiki.wlx.dto.lists.ListConfig.WleUa
import org.scalawiki.wlx.dto.{Contest, Country, Koatuu, Monument}
import org.specs2.mutable.Specification

class MonumentDbSpec extends Specification {

  val Ukraine = Country.Ukraine
  val contest = Contest.WLMUkraine(2014)

  val monuments = Ukraine.regionIds.flatMap {
    regionId =>
      (1 to regionId.toInt).map { i =>
        Monument(
          page = "",
          id = regionId + "-001-" + f"$i%04d",
          name = "Monument in " + Ukraine.regionName(regionId),
          listConfig = Some(WleUa)
        )
      }
  }

  "monument db" should {
    "contain monuments ids" in {
      val db = new MonumentDB(contest, monuments.toSeq)

      db.ids.size === monuments.size
      db.ids === monuments.map(_.id)
    }

    "group monuments by regions" in {
      val db = new MonumentDB(contest, monuments.toSeq)

      val regions = db._byRegion.keySet

      for (region <- regions) yield {
        db._byRegion(region).size === region.toInt
      }

      regions === Ukraine.regionIds
    }

    "get city for Kyiv" in {
      val regionId = "80-391"
      val kyiv = Ukraine.byIdAndName(regionId, "Київ").head
      val places = Seq("Київ") //, "м. Київ", "[[Київ]]", "м. [[Київ]]")

      val monuments = places.zipWithIndex.map { case (city, index) =>
        new Monument(name = "name", id = s"$regionId-$index", city = Some(city))
      }
      val mdb = new MonumentDB(contest, monuments)

      monuments.map { m =>
        val actual = mdb.getAdmDivision(m.id)
        actual aka s"${m.city}" must beSome(kyiv)
      }
    }
  }
}
