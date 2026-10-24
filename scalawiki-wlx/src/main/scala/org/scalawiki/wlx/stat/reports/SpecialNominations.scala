package org.scalawiki.wlx.stat.reports

import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.ImageDB
import org.scalawiki.wlx.dto.{Contest, Country, Monument, SpecialNomination}
import org.scalawiki.wlx.stat.ContestStat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SpecialNominations(stat: ContestStat, imageDb: ImageDB) {

  private val contest: Contest = stat.contest

  def statistics(): Future[Unit] =
    specialNomination().map { wiki =>
      val pageName = s"Commons:${contest.name}/Special nominations statistics"
      MwBot.fromHost(MwBot.commons).page(pageName).edit(wiki, Some("updating"))
      ()
    }

  def nominations: Seq[SpecialNomination] = {
    SpecialNomination.nominations
      .filter(_.years.contains(contest.year))
      .sortBy(_.name)
  }

  def specialNomination(): Future[String] =
    SpecialNomination.getMonumentsMap(nominations, stat).map { monumentsMap =>
      val imageDbs = nominations.map { nomination =>
        val specialNominationImageDb =
          if (monumentsMap.get(nomination).exists(_.nonEmpty)) {
            imageDb.subSet(monumentsMap(nomination), withFalseIds = true)
          } else {
            imageDb.subSet(nomination.matchesFile _)
          }
        nomination -> specialNominationImageDb
      }.toMap

      val headers = Seq(
        "Special nomination",
        "authors",
        "all monuments",
        "special nomination monuments",
        "photographed monuments",
        "photographed special monuments",
        "newly pictured monuments",
        "photos"
      )

      val newImageNames = imageDb.images.map(_.title).toSet
      val oldMonumentIds = stat.totalImageDb.images
        .filterNot(image => newImageNames.contains(image.title))
        .flatMap(_.monumentIds)
        .toSet

      val rows = for (nomination <- nominations) yield {

        val imagesPage =
          s"Commons:Images from ${contest.name} special nomination ${nomination.name}"
        val imageDb = imageDbs(nomination)

        galleryByRegion(imagesPage + " by region", imageDb)
        galleryByAuthor(imagesPage + " by author", imageDb)

        val monumentCounts = monumentsMap.get(nomination).map { monuments =>
          SpecialNominations.monumentCounts(monuments, contest.country.regionIds)
        }

        Seq(
          nomination.name,
          imageDb.authors.size.toString,
          monumentCounts.map(_._1.toString).getOrElse(""),
          monumentCounts.map(_._2.toString).getOrElse(""),
          imageDb.ids.size.toString,
          imageDb.ids.count(isSpecialNominationMonument).toString,
          newlyPicturedText(imagesPage, imageDb, imageDb.ids.diff(oldMonumentIds)),
          s"${imageDb.images.size} [[$imagesPage by region|by region]], [[$imagesPage by author|by author]]"
        )
      }

      val table = Table(headers, rows)

      table.asWiki + s"\n[[Category:${contest.name}]]"
    }

  private def newlyPicturedText(imagesPage: String, imageDb: ImageDB, newMonumentIds: Set[String]): String = {
    val newlyPicturedText = if (newMonumentIds.nonEmpty) {
      val newPicturedImagesPage = imagesPage + " newly pictured"
      galleryByRegion(
        newPicturedImagesPage + " by region",
        imageDb.subSet(i => i.monumentIds.exists(newMonumentIds.contains))
      )
      galleryByAuthor(
        newPicturedImagesPage + " by author",
        imageDb.subSet(i => i.monumentIds.exists(newMonumentIds.contains))
      )
      s"${newMonumentIds.size.toString} [[$newPicturedImagesPage by region|by region]], [[$newPicturedImagesPage by author|by author]]"
    } else {
      newMonumentIds.size.toString
    }
    newlyPicturedText
  }

  private def isSpecialNominationMonument(id: String): Boolean =
    SpecialNominations.isSpecialNominationId(id, contest.country.regionIds)

  private def galleryByRegion(regionsPage: String, imageDb: ImageDB): Unit = {

    val monumentDb = imageDb.monumentDb.get
    var regionsText = ""

    for (region <- Country.Ukraine.regions) {
      val images = imageDb.imagesByRegion(region.code)

      if (images.nonEmpty) {
        val monumentIds = images.flatMap(_.monumentIds)
        val byPlace = monumentIds
          .groupBy { id =>
            monumentDb.placeByMonumentId.getOrElse(id, "Unknown")
          }
          .mapValues(_.toSet)
          .toMap

        val imagesPage = regionsPage + "_" + region.name.replace(' ', '_')
        var imagesText = "__TOC__"
        imagesText += s"\n== ${region.name} ${images.size} images ==\n"
        regionsText += s"*[[$imagesPage|${region.name} ${images.size} images]]\n"
        imagesText += byPlace
          .map { case (code, monumentIds) =>
            val place = Country.Ukraine
              .byMonumentId(monumentIds.head)
              .map(_.name)
              .getOrElse("Unknown")
            val placeImages =
              images.filter(_.monumentIds.toSet.intersect(monumentIds).nonEmpty)
            s"\n=== $place ${placeImages.size} images ===\n" ++
              placeImages
                .map(_.title)
                .mkString("<gallery>\n", "\n", "</gallery>")
          }
          .mkString("\n")

        MwBot
          .fromHost(MwBot.commons)
          .page(imagesPage)
          .edit(imagesText, Some("updating"))
      }
    }

    MwBot
      .fromHost(MwBot.commons)
      .page(regionsPage)
      .edit(regionsText, Some("updating"))
  }

  private def galleryByAuthor(imagesPage: String, imageDb: ImageDB): Unit = {
    var imagesText = "__TOC__"

    val authors = imageDb._byAuthorAndId.grouped.toSeq.sortBy(-_._2.keys.size)
    for ((author, byId) <- authors) {
      val images = imageDb._byAuthor.by(author)
      if (images.nonEmpty) {
        imagesText += s"\n== $author, ${byId.keys.size} monuments ==\n"
        imagesText += images
          .map(_.title)
          .mkString("<gallery>\n", "\n", "</gallery>")
      }
    }

    MwBot
      .fromHost(MwBot.commons)
      .page(imagesPage)
      .edit(imagesText, Some("updating"))
  }
}

object SpecialNominations {

  /** A nomination's monument count and how many of those carry its own
    * special-nomination ids, both over distinct ids: a nomination's lists can
    * list one monument more than once (e.g. wooden architecture had an old and
    * a new set of oblast pages, both embedding the nomination's template), and
    * counting rows would count it every time.
    */
  def monumentCounts(monuments: Seq[Monument], regionIds: Set[String]): (Int, Int) = {
    val ids = monuments.map(_.id).toSet
    (ids.size, ids.count(isSpecialNominationId(_, regionIds)))
  }

  /** An id that doesn't start with a region code, i.e. one assigned by a
    * special nomination's own list rather than the main monument lists. */
  def isSpecialNominationId(id: String, regionIds: Set[String]): Boolean =
    !regionIds.contains(id.split("-").headOption.getOrElse(""))
}
