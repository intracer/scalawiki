package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.dto.Image
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.{ImageDB, MonumentDB}

class AuthorsStat {

  def authorsStat(imageDb: ImageDB, bot: MwBot) {
    new AuthorMonuments(imageDb, rating = imageDb.contest.rating).updateWiki(bot)
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
