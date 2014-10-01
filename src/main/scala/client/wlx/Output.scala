package client.wlx

import client.MwBot
import client.wlx.dto.{Contest, Country, SpecialNomination}

import scala.collection.immutable.SortedSet

class Output {

  def monumentsPictured(imageDbs: Seq[ImageDB], totalImageDb: ImageDB, monumentDb: MonumentDB) = {

    // val contests = (2012 to 2014).map(year => Contest.WLMUkraine(year, "01-09", "31-09"))


    val columns = Seq("Region", "Objects in lists", "3 years total", "3 years percentage", "2012", "2013", "2014")

    val header = "\n==Objects pictured==\n{| class='wikitable sortable'\n" +
      "|+ Objects pictured\n" +
      columns.mkString("!", "!!", "\n")

    val regionIds = SortedSet(monumentDb._byRegion.keySet.toSeq: _*)

    val imageDbsByYear = imageDbs.groupBy(_.contest.year)

    var text = ""
    for (regionId <- regionIds) {
      val columnData = Seq(
        monumentDb.contest.country.regionById(regionId).name,
        monumentDb.byRegion(regionId).size,
        totalImageDb.idsByRegion(regionId).size,
        100 * totalImageDb.idsByRegion(regionId).size / monumentDb.byRegion(regionId).size,
        imageDbsByYear(2012).head.idsByRegion(regionId).size,
        imageDbsByYear(2013).head.idsByRegion(regionId).size,
        imageDbsByYear(2014).head.idsByRegion(regionId).size
      )

      text += columnData.mkString("|-\n| ", " || ", "\n")
    }

    val totalData = Seq(
      "Total",
      monumentDb.monuments.size,
      totalImageDb.ids.size,
      100 * totalImageDb.ids.size / monumentDb.monuments.size,
      imageDbsByYear(2012).head.ids.size,
      imageDbsByYear(2013).head.ids.size,
      imageDbsByYear(2014).head.ids.size
    )
    val total = totalData.mkString("|-\n| ", " || ", "\n|}")

    header + text + total
  }

  def monumentsByType(/*imageDbs: Seq[ImageDB], totalImageDb: ImageDB,*/ monumentDb: MonumentDB) = {
    val regions = monumentDb.contest.country.regionById

    for ((typ, size) <- monumentDb._byType.mapValues(_.size).toSeq.sortBy(-_._2)) {
      val byRegion = monumentDb._byTypeAndRegion(typ)


      val regionStat = byRegion.toSeq.sortBy(-_._2.size).map{
        case (regionId, monuments) =>
          val byReg1 = s"${regions(regionId)}: ${monuments.size}"

          val byReg2 = if (byRegion.size == 1) {
            val byReg2Stat = monuments.groupBy(m => m.id.substring(0,6))

            byReg2Stat.toSeq.sortBy(-_._2.size).map {
              case (regionId2, monuments2) =>
                s"$regionId2: ${monuments2.size}"
            }.mkString("(", ", ", ")")
          } else ""

            byReg1 + byReg2
      }.mkString(", ")
      println(s"$typ: ${monumentDb._byType(typ).size}, $regionStat")
    }
  }


  def authorsContributed(imageDbs: Seq[ImageDB], totalImageDb: ImageDB, monumentDb: MonumentDB) = {

    // val contests = (2012 to 2014).map(year => Contest.WLMUkraine(year, "01-09", "31-09"))


    val columns = Seq("Region", "3 years total", "2012", "2013", "2014")

    val header = "\n==Authors contributed==\n{| class='wikitable sortable'\n" +
      "|+ Authors contributed\n" +
      columns.mkString("!", "!!", "\n")

    val regionIds = SortedSet(monumentDb._byRegion.keySet.toSeq: _*)

    val imageDbsByYear = imageDbs.groupBy(_.contest.year)

    var text = ""
    for (regionId <- regionIds) {
      val columnData = Seq(
        monumentDb.contest.country.regionById(regionId).name,
        totalImageDb.authorsByRegion(regionId).size,
        imageDbsByYear(2012).head.authorsByRegion(regionId).size,
        imageDbsByYear(2013).head.authorsByRegion(regionId).size,
        imageDbsByYear(2014).head.authorsByRegion(regionId).size
      )

      text += columnData.mkString("|-\n| ", " || ", "\n")
    }

    val totalData = Seq(
      "Total",
      totalImageDb.authors.size,
      imageDbsByYear(2012).head.authors.size,
      imageDbsByYear(2013).head.authors.size,
      imageDbsByYear(2014).head.authors.size
    )
    val total = totalData.mkString("|-\n| ", " || ", "\n|}")

    header + text + total

  }

  def specialNomination(contest: Contest, imageDbs: Map[SpecialNomination, ImageDB]) = {

    val columns = Seq("Special nomination", "authors", "monuments", "photos")

    val header = "{| class='wikitable sortable'\n" +
      "|+ Special nomination statistics\n" +
      columns.mkString("!", "!!", "\n")

    var text = ""
    val nominations: Seq[SpecialNomination] = imageDbs.keySet.toSeq.sortBy(_.name)
    for (nomination <- nominations) {

      val imagesPage = s"Commons:Images from Wiki Loves Monuments ${contest.year} in Ukraine special nomination ${nomination.name}"

      val imageDb = imageDbs(nomination)
      val columnData = Seq(
        nomination.name,
        imageDb.authors.size,
        imageDb.ids.size,
        s"[[$imagesPage|${imageDb.images.size}]]"
      )

      var imagesText = "__TOC__"

      for (region <- Country.Ukraine.regions) {
        val images = imageDb.imagesByRegion(region.code)
        if (images.nonEmpty) {
          imagesText += s"\n== ${region.name} ${images.size} images ==\n"
          imagesText += images.map(_.title).mkString("<gallery>\n", "\n", "</gallery>")
        }
      }

      MwBot.get(MwBot.commons).page(imagesPage).edit(imagesText, "updating")

      text += columnData.mkString("|-\n| ", " || ", "\n")
    }

    val total = "|}" + s"\n[[Category:Wiki Loves Monuments ${contest.year} in Ukraine]]"

    header + text + total
  }

  def authorsMonuments(imageDb: ImageDB) = {

    val country = imageDb.contest.country
    val columns = Seq("User", "Objects pictured", "Photos uploaded") ++ country.regionNames

    val header = "{| class='wikitable sortable'\n" +
      "|+ Number of objects pictured by uploader\n" +
      columns.mkString("!", "!!", "\n")

    var text = ""
    val totalData = Seq(
      "Total",
      imageDb.ids.size,
      imageDb.images.size
    ) ++ country.regionIds.toSeq.map(regId => imageDb.idsByRegion(regId).size)

    text += totalData.mkString("|-\n| ", " || ", "\n")

    val authors = imageDb.authors.toSeq.sortBy(user => -imageDb._authorsIds(user).size)
    for (user <- authors) {
      val columnData = Seq(
        user.replaceAll("\\{\\{", "").replaceAll("\\}\\}", ""),
        imageDb._authorsIds(user).size,
        imageDb._byAuthor(user).size
      ) ++ country.regionIds.toSeq.map(regId => imageDb._authorIdsByRegion(user).getOrElse(regId, Seq.empty).size)

      text += columnData.mkString("|-\n| ", " || ", "\n")
    }


    val total = "|}"

    header + text + total
  }

}
