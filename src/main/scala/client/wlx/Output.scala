package client.wlx

import client.wlx.dto.Region

import scala.collection.immutable.SortedSet

class Output {

  def monumentsPictured(imageDbs: Seq[ImageDB], totalImageDb: ImageDB, monumentsDb:  MonumentDB) = {

   // val contests = (2012 to 2014).map(year => Contest.WLMUkraine(year, "01-09", "31-09"))


    val columns = Seq("Region", "3 years total", "3 years percentage" ,"2012", "2013", "2014")

    val header = "{| class='wikitable sortable\n" +
      "|+ Objects pictured\n" +
      columns.mkString("!", "!!", "\n" )

    val regionIds = SortedSet(monumentsDb._byRegion.keySet.toSeq:_*)

    val imageDbsByYear = imageDbs.groupBy(_.contest.year)

    var text = "|-\n"
    for (regionId <- regionIds) {
//      val total = imageDbsByYear(2014).head.images
      text += s"| ${Region.Ukraine(regionId)} || || || ||"
    }

  }


}
