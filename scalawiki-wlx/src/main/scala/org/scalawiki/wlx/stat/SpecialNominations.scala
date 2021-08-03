package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.ImageDB
import org.scalawiki.wlx.dto.{Contest, Country, Koatuu, Monument, SpecialNomination}
import org.scalawiki.wlx.query.MonumentQuery

class SpecialNominations(stat: ContestStat, imageDb: ImageDB) {

  val contest = stat.contest

  def statistics(): Unit = {

    val stat = specialNomination()

    val pageName = s"Commons:${contest.name}/Special nominations statistics"
    MwBot.fromHost(MwBot.commons).page(pageName).edit(stat, Some("updating"))
  }

  def nominations: Seq[SpecialNomination] = {
    SpecialNomination.nominations.filter(_.years.contains(contest.year)).sortBy(_.name)
  }

  def specialNomination(): String = {
    val monumentsMap = SpecialNomination.getMonumentsMap(nominations, stat)
    val imageDbs = nominations.map { nomination =>
      nomination -> imageDb.subSet(monumentsMap(nomination), withFalseIds = true)
    }.toMap

    val headers = Seq("Special nomination", "authors",
      "all monuments", "special nomination monuments", "photographed monuments", "photographed special monuments",
      "newly pictured monuments", "photos")

    val newImageNames = imageDb.images.map(_.title).toSet
    val oldMonumentIds = stat.totalImageDb.get.images
      .filterNot(image => newImageNames.contains(image.title))
      .flatMap(_.monumentIds).toSet

    val rows = for (nomination <- nominations) yield {

      val imagesPage = s"Commons:Images from ${contest.name} special nomination ${nomination.name}"
      val imageDb = imageDbs(nomination)

      galleryByRegion(imagesPage + " by region", imageDb)
      galleryByAuthor(imagesPage + " by author", imageDb)

      Seq(
        nomination.name,
        imageDb.authors.size.toString,
        monumentsMap(nomination).size.toString,
        monumentsMap(nomination).map(_.id).count(isSpecialNominationMonument).toString,
        imageDb.ids.size.toString,
        imageDb.ids.count(isSpecialNominationMonument).toString,
        imageDb.ids.diff(oldMonumentIds).size.toString,
        s"${imageDb.images.size} [[$imagesPage by region|by region]], [[$imagesPage by author|by author]]"
      )
    }

    val table = new Table(headers, rows)

    table.asWiki + s"\n[[Category:${contest.name}]]"
  }

  private def isSpecialNominationMonument(id: String) = {
    val regionId = id.split("-").headOption.getOrElse("")
    !contest.country.regionIds.contains(regionId)
  }

  def galleryByRegion(imagesPage: String, imageDb: ImageDB): Unit = {
    var imagesText = "__TOC__"
    val monumentDb = imageDb.monumentDb.get

    for (region <- Country.Ukraine.regions) {
      val images = imageDb.imagesByRegion(region.code)

      if (images.nonEmpty) {
        val monumentIds = images.flatMap(_.monumentIds)
        val byPlace = monumentIds.groupBy { id =>
          monumentDb.placeByMonumentId.getOrElse(id, "Unknown")
        }.mapValues(_.toSet).toMap

        imagesText += s"\n== ${region.name} ${images.size} images ==\n"

        imagesText += byPlace.map { case (code, monumentIds) =>
          val place = Country.Ukraine.byId(monumentIds.head).map(_.name).getOrElse("Unknown")
          val placeImages = images.filter(_.monumentIds.toSet.intersect(monumentIds).nonEmpty)
          s"\n=== $place ${placeImages.size} images ===\n" ++
            placeImages.map(_.title).mkString("<gallery>\n", "\n", "</gallery>")
        }.mkString("\n")
      }
    }

    MwBot.fromHost(MwBot.commons).page(imagesPage).edit(imagesText, Some("updating"))
  }

  def galleryByAuthor(imagesPage: String, imageDb: ImageDB): Unit = {
    var imagesText = "__TOC__"

    val authors = imageDb._byAuthorAndId.grouped.toSeq.sortBy(-_._2.keys.size)
    for ((author, byId) <- authors) {
      val images = imageDb._byAuthor.by(author)
      if (images.nonEmpty) {
        imagesText += s"\n== $author, ${byId.keys.size} monuments ==\n"
        imagesText += images.map(_.title).mkString("<gallery>\n", "\n", "</gallery>")
      }
    }

    MwBot.fromHost(MwBot.commons).page(imagesPage).edit(imagesText, Some("updating"))
  }
}