package org.scalawiki.wlx.stat.reports

import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.stat.ContestStat
import org.scalawiki.wlx.{ImageDB, MonumentDB}

class MostPopularMonuments(val stat: ContestStat) extends Reporter {

  def this(imageDbs: Seq[ImageDB], totalImageDb: Option[ImageDB], monumentDb: MonumentDB) = {
    this(ContestStat(
      monumentDb.contest,
      imageDbs.headOption.map(_.contest.year).getOrElse(monumentDb.contest.year),
      Some(monumentDb),
      imageDbs.lastOption.orElse(totalImageDb),
      totalImageDb,
      imageDbs//.headOption.map(_ => imageDbs.init).getOrElse(Seq.empty)
    ))
  }

  val name = "Most photographed objects"

  override def category = contest.contestType.name + " in " + contest.country.name

  def table =
    mostPopularMonumentsTable(stat.dbsByYear, stat.totalImageDb, stat.monumentDb.get)

  def mostPopularMonumentsTable(imageDbs: Seq[ImageDB], totalImageDb: Option[ImageDB], monumentDb: MonumentDB): Table = {
    val imageDbsByYear = imageDbs.groupBy(_.contest.year)

    val yearSeq = imageDbsByYear.keys.toSeq.sorted
    val numYears = yearSeq.size

    val columns = Seq("Id", "Name") ++
      totalImageDb.map(_ => Seq(s"$numYears years photos", s"$numYears years authors")).getOrElse(Seq.empty) ++
      yearSeq.flatMap(year => Seq(s"$year photos", s"$year authors"))

    val photosCountTotal = totalImageDb.map(_.imageCountById)
    val authorsCountTotal = totalImageDb.map(_.authorsCountById)

    val photoCounts = yearSeq.map(year => imageDbsByYear(year).head.imageCountById)
    val authorCounts = yearSeq.map(year => imageDbsByYear(year).head.authorsCountById)

    val counts = Seq(photosCountTotal, authorsCountTotal).flatten ++
      (0 until numYears).flatMap(i => Seq(photoCounts(i), authorCounts(i)))

    val topPhotos = (photosCountTotal.toSet ++ photoCounts).flatMap(topN(12, _).toSet)
    val topAuthors = (authorsCountTotal.toSet ++ authorCounts).flatMap(topN(12, _).toSet)

    val allTop = topPhotos ++ topAuthors
    val allTopOrdered = allTop.toSeq.sorted


    val rows = allTopOrdered.map { id =>
      val monument = monumentDb.byId(id).get
      Seq(
        id,
        monument.name.replaceAll("\\[\\[", "[[:uk:") + monument.galleryLink
      ) ++ counts.map(_.getOrElse(id, 0).toString)
    }

    new Table(columns, rows, name)
  }

  def topN(n: Int, stat: Map[String, Int]) = stat.toSeq.sortBy(-_._2).take(n).map(_._1)

}
