package client.wlx

import client.wlx.dto.Region

import scala.collection.immutable.SortedSet

class Output {

  def monumentsPictured(imageDbs: Seq[ImageDB], totalImageDb: ImageDB, monumentsDb:  MonumentDB) = {

   // val contests = (2012 to 2014).map(year => Contest.WLMUkraine(year, "01-09", "31-09"))


    val columns = Seq("Region", "Objects in lists", "3 years total", "3 years percentage" ,"2012", "2013", "2014")

    val header = "{| class='wikitable sortable\n" +
      "|+ Objects pictured\n" +
      columns.mkString("!", "!!", "\n" )

    val regionIds = SortedSet(monumentsDb._byRegion.keySet.toSeq:_*)

    val imageDbsByYear = imageDbs.groupBy(_.contest.year)

    var text = ""
    for (regionId <- regionIds) {
      val columnData = Seq(
        Region.Ukraine(regionId),
          monumentsDb.byRegion(regionId).size,
          totalImageDb.byRegion(regionId).size,
          100 * totalImageDb.byRegion(regionId).size / monumentsDb.byRegion(regionId).size,
          imageDbsByYear(2012).head.byRegion(regionId),
          imageDbsByYear(2013).head.byRegion(regionId),
          imageDbsByYear(2014).head.byRegion(regionId)
      )

      text += columnData.mkString("|-\n| ", " || ","\n")
    }

    val totalData = Seq(
      "Total",
      monumentsDb.monuments.size,
      totalImageDb.ids.size,
      100 * totalImageDb.ids.size / monumentsDb.monuments.size,
      imageDbsByYear(2012).head.ids.size,
      imageDbsByYear(2013).head.ids.size,
      imageDbsByYear(2014).head.ids.size
    )
    val total = totalData.mkString("|-\n| "," || " ,"\n|}")

    header + text + total

  }


}
