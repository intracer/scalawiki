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

  private val statisticsPage = s"Commons:${contest.name}/Special nominations statistics"
  private val duplicatedIdsPage = s"Commons:${contest.name}/Special nominations duplicated ids"

  /** Writes the statistics table, and the ids repeated within a nomination's
    * own lists. */
  def statistics(): Future[Unit] =
    SpecialNomination.getNominationMonuments(nominations, stat).map { nominationMonuments =>
      val commons = MwBot.fromHost(MwBot.commons)
      val monumentsMap = nominationMonuments.map { case (nomination, m) => nomination -> m.all }
      val duplicates = nominations.flatMap { nomination =>
        nominationMonuments.get(nomination).map(m => nomination.name -> SpecialNominations.duplicatedIds(m.fromLists))
      }
      commons.page(statisticsPage).edit(specialNomination(monumentsMap), Some("updating"))
      commons
        .page(duplicatedIdsPage)
        .edit(SpecialNominations.duplicatedIdsText(duplicates, contest.name), Some("updating"))
      ()
    }

  def nominations: Seq[SpecialNomination] = {
    SpecialNomination.nominations
      .filter(_.years.contains(contest.year))
      .sortBy(_.name)
  }

  def specialNomination(monumentsMap: Map[SpecialNomination, Seq[Monument]]): String = {
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

    table.asWiki +
      s"\n\nIds repeated within a nomination's own lists: [[$duplicatedIdsPage]]\n" +
      s"\n[[Category:${contest.name}]]"
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

  /** Ids given to more than one row of a nomination's own lists, sorted, each
    * with its rows in list order. Rows without an id are left out. Pass only the
    * nomination's own list rows: a monument a thematic list shares with a main
    * list, or with another nomination, is listed there on purpose.
    */
  def duplicatedIds(listMonuments: Seq[Monument]): Seq[(String, Seq[Monument])] =
    listMonuments
      .filter(_.id.trim.nonEmpty)
      .groupBy(_.id.trim)
      .collect { case (id, ms) if ms.size > 1 => id -> listMonuments.filter(_.id.trim == id) }
      .toSeq
      .sortBy(_._1)

  private val listsPrefix = "Вікіпедія:Вікі любить пам'ятки/"

  /** The duplicated ids page: a section per nomination that has any, a table
    * row per id with every row that carries it. Monument names and list pages
    * come from uk.wikipedia, so their links get the `:uk:` interwiki prefix.
    */
  def duplicatedIdsText(byNomination: Seq[(String, Seq[(String, Seq[Monument])])], contestName: String): String = {
    val withDuplicates = byNomination.filter(_._2.nonEmpty)
    val intro =
      "Ids given to more than one row of the same special nomination's own lists. " +
        "A monument listed both in a nomination and in the main monument lists, " +
        "or in two different nominations, is not counted here.\n"
    val sections =
      if (withDuplicates.isEmpty) "\nNo duplicated ids.\n"
      else
        withDuplicates.map { case (nomination, duplicates) =>
          val rows = duplicates.map { case (id, monuments) =>
            val occurrences = monuments.map { m =>
              val name = m.name.replace("[[", "[[:uk:").split("\\s+").filter(_.nonEmpty).mkString(" ")
              val page =
                if (m.page.nonEmpty) s" — [[:uk:${m.page}|${m.page.stripPrefix(listsPrefix)}]]" else ""
              s"$name$page"
            }
            Seq(id, monuments.size.toString, occurrences.mkString("<br>"))
          }
          s"\n== $nomination ==\n" + Table(Seq("ID", "rows", "rows with this id"), rows).asWiki + "\n"
        }.mkString

    intro + sections + s"\n[[Category:$contestName]]"
  }
}
