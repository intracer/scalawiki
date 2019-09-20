package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.ImageDB
import org.scalawiki.wlx.dto.{Contest, Country, Monument, SpecialNomination}
import org.scalawiki.wlx.query.MonumentQuery

class SpecialNominations(contest: Contest, imageDb: ImageDB) {

  def statistics(): Unit = {

    val stat = specialNomination()

    val pageName = s"Commons:${contest.name}/Special nominations statistics"
    MwBot.fromHost(MwBot.commons).page(pageName).edit(stat, Some("updating"))
  }

  def nominations: Seq[SpecialNomination] = {
    SpecialNomination.nominations.filter(_.years.contains(contest.year)).sortBy(_.name)
  }

  def getMonumentsMap(monumentQuery: MonumentQuery): Map[SpecialNomination, Seq[Monument]] = {
    nominations.map { nomination =>
      val monuments = monumentQuery.byPage(nomination.pages.head, nomination.listTemplate)
      (nomination, monuments)
    }.toMap
  }

  def specialNomination(): String = {
    val monumentQuery = MonumentQuery.create(contest)

    val imageDbs = nominations.map { nomination =>
      nomination -> imageDb.subSet(getMonumentsMap(monumentQuery)(nomination), withFalseIds = true)
    }.toMap

    val headers = Seq("Special nomination", "authors", "all monuments", "special monuments", "photos")
    val rows = for (nomination <- nominations) yield {

      val imagesPage = s"Commons:Images from ${contest.name} special nomination ${nomination.name}"
      val imageDb = imageDbs(nomination)

      makeSpecNominationGallery(imagesPage, imageDb)

      Seq(
        nomination.name,
        imageDb.authors.size.toString,
        imageDb.ids.size.toString,
        imageDb.ids.filterNot { id =>
          val regionId = id.split("-").headOption.getOrElse("")
          contest.country.regionIds.contains(regionId)
        }.size.toString,
        s"[[$imagesPage|${imageDb.images.size}]]"
      )
    }

    val table = new Table(headers, rows)

    table.asWiki + s"\n[[Category:${contest.name}]]"
  }

  def makeSpecNominationGallery(imagesPage: String, imageDb: ImageDB): Unit = {
    var imagesText = "__TOC__"

    for (region <- Country.Ukraine.regions) {
      val images = imageDb.imagesByRegion(region.code)
      if (images.nonEmpty) {
        imagesText += s"\n== ${region.name} ${images.size} images ==\n"
        imagesText += images.map(_.title).mkString("<gallery>\n", "\n", "</gallery>")
      }
    }

    MwBot.fromHost(MwBot.commons).page(imagesPage).edit(imagesText, Some("updating"))
  }
}

object SpecialNominations {
  def main(args: Array[String]) {
    val contest = Contest.WLMUkraine(2015)
    val query = MonumentQuery.create(contest)
    val map = new SpecialNominations(contest, new ImageDB(contest, Seq.empty)).getMonumentsMap(query)
    println(map.values.map(_.size).mkString(", "))
  }
}