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
    val contestPage = s"${contest.contestType.name} ${contest.year} in ${contest.country.name}"


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

  def authorsContributed(imageDbs: Seq[ImageDB], totalImageDb: ImageDB, monumentDb: Option[MonumentDB]) = {

    val table = authorsContributedTable(imageDbs, totalImageDb, monumentDb)

    val header = "\n==Authors contributed==\n"
    header + table.asWiki
  }

  def authorsContributedTable(imageDbs: Seq[ImageDB], totalImageDb: ImageDB, monumentDb: Option[MonumentDB]): Table = {
    val imageDbsByYear = imageDbs.groupBy(_.contest.year)
    val yearSeq = imageDbsByYear.keys.toSeq.sorted
    val numYears = yearSeq.size

    val columns = Seq("Region", s"$numYears years total") ++ yearSeq.map(_.toString)

    val totalData = (Seq(
      "Total",
      totalImageDb.authors.size) ++ yearSeq.map(year => imageDbsByYear(year).head.authors.size)).map(_.toString)


    val perRegion = monumentDb.fold(Seq.empty[Seq[String]]) {
      db =>
        val contest = db.contest
        val regionIds = db._byRegion.keySet.toSeq
          .filter(contest.country.regionIds.contains).sorted

        regionIds.map {
          regionId =>
            val nameAndTotal = Seq(
              contest.country.regionName(regionId),
              totalImageDb.authorsByRegion(regionId).size.toString
            )
            val totalByYear = yearSeq.map { year =>
              imageDbsByYear(year).headOption
                .fold("")(_.authorsByRegion(regionId).size.toString)
            }

            nameAndTotal ++ totalByYear
        }
    }

    val rows = perRegion ++ Seq(totalData)

    //    val authors = yearSeq.map(year => imageDbsByYear(year).head.authors)
    //    val filename = contest.contestType.name.split(" ").mkString + "In" + contest.country.name + "AuthorsByYearPie"
    //    charts.intersectionDiagram("Унікальність авторів за роками", filename, yearSeq, authors, 900, 900)

    new Table(columns, rows, "Authors contributed")
  }

  def authorsMonumentsTable(imageDb: ImageDB, rating: Boolean = false): Table = {

    val contest = imageDb.contest
    val country = contest.country
    val columns = Seq("User") ++
      (if (rating) Seq("Objects pictured", "Existing", "New", "Rating")
      else Seq("Objects pictured")) ++
      Seq("Photos uploaded") ++ country.regionNames

    val oldIds = imageDb.oldMonumentDb.fold(Set.empty[String])(_.withImages.map(_.id).toSet)

    def ratingData(ids: Set[String], oldIds: Set[String]): Seq[Int] = {
      Seq(
        ids.size,
        (ids intersect oldIds).size,
        (ids -- oldIds).size,
        ids.size + (ids -- oldIds).size
      )
    }

    def rowData(ids: Set[String], images: Int, regionData: String => Int, rating: Boolean = false): Seq[Int] = {
      (if (rating) {
        ratingData(ids, oldIds)
      } else {
        Seq(ids.size)
      }) ++
        Seq(images) ++ country.regionIds.toSeq.map(regId => regionData(regId))
    }

    val totalData = Seq("Total") ++
      rowData(imageDb.ids, imageDb.images.size, regId => imageDb.idsByRegion(regId).size).map(_.toString)

    val authors = imageDb.authors.toSeq.sortBy(user => -imageDb._authorsIds(user).size)
    val authorsData = authors.map { user =>
      val noTemplateUser = user.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")
      val userLink = s"[[User:$noTemplateUser|$noTemplateUser]]"

      def userRating(regId: String) = {
        val regionIds = imageDb._authorIdsByRegion(user).getOrElse(regId, Seq.empty).toSet
        if (rating) {
          regionIds.size + (regionIds -- oldIds).size
        } else {
          regionIds.size
        }
      }
      Seq(userLink) ++
        rowData(imageDb._authorsIds(user), imageDb._byAuthor(user).size, userRating).map(_.toString)
    }

    new Table(columns, Seq(totalData) ++ authorsData, "Number of objects pictured by uploader")
  }

  def authorsMonuments(imageDb: ImageDB, rating: Boolean = false): String = {
    val table = authorsMonumentsTable(imageDb, rating)
    val contest = imageDb.contest
    table.asWiki + s"\n[[Category:${contest.contestType.name} ${contest.year} in ${contest.country.name}]]"
  }

  def authorsImages(byAuthor: Map[String, Seq[Image]], monumentDb: Option[MonumentDB]) = {

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
