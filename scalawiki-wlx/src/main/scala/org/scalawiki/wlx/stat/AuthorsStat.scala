package org.scalawiki.wlx.stat

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import org.scalawiki.MwBot
import org.scalawiki.dto.Image
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.{ImageDB, MonumentDB}

class AuthorsStat {

  def authorsStat(imageDb: ImageDB, bot: MwBot) {
    val contest = imageDb.contest
    val contestPage = contest.name


    val numberOfMonuments = authorsMonuments(imageDb)

    Files.write(Paths.get("authorsMonuments.txt"), numberOfMonuments.getBytes(StandardCharsets.UTF_8))
    bot.page(s"Commons:$contestPage/Number of objects pictured by uploader")
      .edit(numberOfMonuments, Some("updating"))

    if (contest.rating) {
      val rating = authorsMonuments(imageDb, rating = true)
      Files.write(Paths.get("authorsRating.txt"), rating.getBytes(StandardCharsets.UTF_8))
      bot.page(s"Commons:$contestPage/Rating based on number and originality of objects pictured by uploader")
        .edit(rating, Some("updating"))

    }
  }

  def authorsContributed(imageDbs: Seq[ImageDB], totalImageDb: Option[ImageDB], monumentDb: Option[MonumentDB]) = {

    val table = authorsContributedTable(imageDbs, totalImageDb, monumentDb)

    val header = "\n==Authors contributed==\n"
    header + table.asWiki
  }

  def authorsContributedTable(imageDbs: Seq[ImageDB], totalImageDb: Option[ImageDB], monumentDb: Option[MonumentDB]): Table = {
    val imageDbsByYear = imageDbs.groupBy(_.contest.year)
    val yearSeq = imageDbsByYear.keys.toSeq.sorted

    val numYears = yearSeq.size

    val dbs = totalImageDb.toSeq ++ yearSeq.flatMap { year => imageDbsByYear(year).headOption }

    val columns = Seq("Region") ++
      totalImageDb.map(_ => s"$numYears years total").toSeq ++
      yearSeq.map(_.toString)

    val perRegion = monumentDb.fold(Seq.empty[Seq[String]]) {
      db =>
        val country = db.contest.country
        db.regionIds.map {
          regionId =>
            val regionName = country.regionName(regionId)

            Seq(regionName) ++ dbs.map(_.authorsByRegion(regionId).size.toString)
        }
    }

    val totalData = Seq("Total") ++ dbs.map(_.authors.size.toString)

    val rows = perRegion ++ Seq(totalData)

    new Table(columns, rows, "Authors contributed")
  }

  def authorsMonumentsTable(imageDb: ImageDB, rating: Boolean = false): Table = {

    val country = imageDb.contest.country
    val columns = Seq("User", "Objects pictured") ++
      (if (rating) Seq("Existing", "New", "Rating") else Seq.empty) ++
      Seq("Photos uploaded") ++
      country.regionNames

    def ratingFunc(allIds: Set[String], oldIds: Set[String]): Int =
      if (rating)
        allIds.size + (allIds -- oldIds).size
      else
        allIds.size

    val oldIds = imageDb.oldMonumentDb.fold(Set.empty[String])(_.withImages.map(_.id).toSet)

    def rowData(ids: Set[String], images: Int, regionRating: String => Int, rating: Boolean = false): Seq[Int] = {
      Seq(ids.size) ++
        (if (rating) {
          Seq(
            (ids intersect oldIds).size,
            (ids -- oldIds).size,
            ratingFunc(ids, oldIds)
          )
        } else Seq.empty[Int]) ++
        Seq(images) ++
        country.regionIds.toSeq.map(regionRating)
    }

    val totalData = Seq("Total") ++
      rowData(imageDb.ids, imageDb.images.size, regId => imageDb.idsByRegion(regId).size, rating).map(_.toString)

    val authors = imageDb.authors.toSeq.sortBy(user => -imageDb._authorsIds(user).size)
    val authorsData = authors.map { user =>
      val noTemplateUser = user.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")
      val userLink = s"[[User:$noTemplateUser|$noTemplateUser]]"

      def userRating(regId: String) = {
        val regionIds = imageDb._authorIdsByRegion(user).getOrElse(regId, Seq.empty).toSet
        ratingFunc(regionIds, oldIds)
      }
      Seq(userLink) ++
        rowData(imageDb._authorsIds(user), imageDb._byAuthor(user).size, userRating, rating).map(_.toString)
    }

    new Table(columns, Seq(totalData) ++ authorsData, "Number of objects pictured by uploader")
  }

  def authorsMonuments(imageDb: ImageDB, rating: Boolean = false): String = {
    val table = authorsMonumentsTable(imageDb, rating)
    val contest = imageDb.contest
    table.asWiki + s"\n[[Category:${contest.name}]]"
  }

  def authorsImages(byAuthor: Map[String, Seq[Image]], monumentDb: Option[MonumentDB]): String = {

    val sections = byAuthor
      .mapValues(images => monumentDb.fold(images)(db => images.filter(_.monumentId.fold(false)(db.ids.contains))))
      .collect {
        case (user, images) if images.nonEmpty =>
          val userLink = s"[[User:$user|$user]]"
          val header = s"== $userLink =="
          val descriptions = images.map(i => i.mpx + " " + i.resolution)

          header + Image.gallery(images.map(_.title), descriptions)
      }

    sections.mkString("__TOC__\n", "\n", "")
  }

}
