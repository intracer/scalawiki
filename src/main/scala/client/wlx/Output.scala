package client.wlx

import client.wlx.dto.{SpecialNomination, Region}

import scala.collection.immutable.SortedSet

class Output {

  def monumentsPictured(imageDbs: Seq[ImageDB], totalImageDb: ImageDB, monumentsDb:  MonumentDB) = {

   // val contests = (2012 to 2014).map(year => Contest.WLMUkraine(year, "01-09", "31-09"))


    val columns = Seq("Region", "Objects in lists", "3 years total", "3 years percentage" ,"2012", "2013", "2014")

    val header = "{| class='wikitable sortable'\n" +
      "|+ Objects pictured\n" +
      columns.mkString("!", "!!", "\n" )

    val regionIds = SortedSet(monumentsDb._byRegion.keySet.toSeq:_*)

    val imageDbsByYear = imageDbs.groupBy(_.contest.year)

    var text = ""
    for (regionId <- regionIds) {
      val columnData = Seq(
        Region.Ukraine(regionId),
          monumentsDb.byRegion(regionId).size,
          totalImageDb.idsByRegion(regionId).size,
          100 * totalImageDb.idsByRegion(regionId).size / monumentsDb.byRegion(regionId).size,
          imageDbsByYear(2012).head.idsByRegion(regionId).size,
          imageDbsByYear(2013).head.idsByRegion(regionId).size,
          imageDbsByYear(2014).head.idsByRegion(regionId).size
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
    val total = totalData.mkString("|-\n| "," || " ,"\n|}")  + "\n[[Category:Wiki Loves Monuments 2014 in Ukraine]]"

    header + text + total

  }

  def authorsContributed(imageDbs: Seq[ImageDB], totalImageDb: ImageDB, monumentsDb:  MonumentDB) = {

    // val contests = (2012 to 2014).map(year => Contest.WLMUkraine(year, "01-09", "31-09"))


    val columns = Seq("Region", "3 years total", "2012", "2013", "2014")

    val header = "{| class='wikitable sortable'\n" +
      "|+ Authors contributed\n" +
      columns.mkString("!", "!!", "\n" )

    val regionIds = SortedSet(monumentsDb._byRegion.keySet.toSeq:_*)

    val imageDbsByYear = imageDbs.groupBy(_.contest.year)

    var text = ""
    for (regionId <- regionIds) {
      val columnData = Seq(
        Region.Ukraine(regionId),
        totalImageDb.authorsByRegion(regionId).size,
        imageDbsByYear(2012).head.authorsByRegion(regionId).size,
        imageDbsByYear(2013).head.authorsByRegion(regionId).size,
        imageDbsByYear(2014).head.authorsByRegion(regionId).size
      )

      text += columnData.mkString("|-\n| ", " || ","\n")
    }

    val totalData = Seq(
      "Total",
      totalImageDb.authors.size,
      imageDbsByYear(2012).head.authors.size,
      imageDbsByYear(2013).head.authors.size,
      imageDbsByYear(2014).head.authors.size
    )
    val total = totalData.mkString("|-\n| "," || " ,"\n|}") + "\n[[Category:Wiki Loves Monuments 2014 in Ukraine]]"

    header + text + total

  }

  def specialNomination(imageDbs: Map[SpecialNomination, ImageDB]) = {

    val columns = Seq("Special nomination", "authors", "monuments", "photos", "")

    val header = "{| class='wikitable sortable'\n" +
      "|+ Special nomination statistics\n" +
      columns.mkString("!", "!!", "\n" )

    var text = ""
    val nominations: Seq[SpecialNomination] = imageDbs.keySet.toSeq.sortBy(_.name)
    for (nomination <- nominations) {
      val imageDb = imageDbs(nomination)
      val columnData = Seq(
        nomination.name,
        imageDb.authors.size,
        imageDb.ids.size,
        imageDb.images.size
      )

      text += columnData.mkString("|-\n| ", " || ","\n")
    }

    val total = "|}" + "\n[[Category:Wiki Loves Monuments 2014 in Ukraine]]"

    header + text + total
  }



}
