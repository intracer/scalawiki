package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.wlx.ImageDB
import org.scalawiki.wlx.dto.{Country, SpecialNomination, Contest}
import org.scalawiki.wlx.query.MonumentQuery

class SpecialNominations {

  def specialNominations(contest: Contest, imageDb: ImageDB, monumentQuery: MonumentQuery) {
    val monumentsMap = SpecialNomination.nominations.map { nomination =>
      val monuments = monumentQuery.byPage(nomination.pages.head, nomination.listTemplate)
      (nomination, monuments)
    }.toMap

    val imageDbs: Map[SpecialNomination, ImageDB] = SpecialNomination.nominations.map { nomination =>
      (nomination, imageDb.subSet(monumentsMap(nomination)))
    }.toMap

    val stat = specialNomination(contest, imageDbs)

    MwBot.get(MwBot.commons).page(s"Commons:Wiki Loves ${contest.contestType.name} ${contest.year} in ${contest.country.name}/Special nominations statistics").edit(stat, Some("updating"))
  }

  def specialNomination(contest: Contest, imageDbs: Map[SpecialNomination, ImageDB]) = {

    val columns = Seq("Special nomination", "authors", "monuments", "photos")

    val header = "{| class='wikitable sortable'\n" +
      "|+ Special nomination statistics\n" +
      columns.mkString("!", "!!", "\n")

    var text = ""
    val nominations: Seq[SpecialNomination] = imageDbs.keySet.toSeq.sortBy(_.name)
    for (nomination <- nominations) {

      val imagesPage = s"Commons:Images from Wiki Loves ${contest.contestType.name} ${contest.year} in ${contest.country.name} special nomination ${nomination.name}"

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

      MwBot.get(MwBot.commons).page(imagesPage).edit(imagesText, Some("updating"))

      text += columnData.mkString("|-\n| ", " || ", "\n")
    }

    val total = "|}" + s"\n[[Category:Wiki Loves ${contest.contestType.name} ${contest.year} in ${contest.country.name}]]"

    header + text + total
  }


}
