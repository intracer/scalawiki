package org.scalawiki.wlx.stat
import org.jfree.data.category.DefaultCategoryDataset
import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.{ImageDB, MonumentDB}

class MonumentsPicturedByRegion(val stat: ContestStat, uploadImages: Boolean = false)  extends Reporter {

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

  val name = "Monuments pictured by region"

  val yearSeq = stat.yearSeq

  override def asText: String = {

    val header = s"\n==$name==\n"

    header + table.asWiki + regionalStatImages().getOrElse("")
  }

  def table = monumentsPicturedTable(stat.dbsByYear, stat.totalImageDb, stat.monumentDb.get)

  def monumentsPicturedTable(imageDbs: Seq[ImageDB], totalImageDb: Option[ImageDB], monumentDb: MonumentDB): Table = {

    val columns = {
      val numYears = yearSeq.size
      val yearsColumns = yearSeq.flatMap { year =>
        val ys = year.toString
        Seq(ys + " Objects", ys + " Pictures")
      }

      Seq("Region", "Objects in lists", s"$numYears years total", s"$numYears years percentage") ++ yearsColumns
    }

    def regionData(regionId:String) = {
      val picturedMonumentsInRegionSet = totalImageDb.map(_.idsByRegion(regionId)).getOrElse(Set.empty) ++
        monumentDb.picturedInRegion(regionId)
      val picturedMonumentsInRegion = picturedMonumentsInRegionSet.size
      val allMonumentsInRegion = monumentDb.byRegion(regionId).size

      val pictured = stat.mapYears { db =>
        Seq(
          db.idsByRegion(regionId).size,
          db.imagesByRegion(regionId).toSet.size
        )
      }.flatten

      val regionName = contest.country.regionName(regionId)
      val columnData = (Seq(
        regionName,
        allMonumentsInRegion,
        picturedMonumentsInRegion,
        100 * picturedMonumentsInRegion / allMonumentsInRegion) ++ pictured).map(_.toString)

      columnData
    }

    val regionIds = monumentDb.regionIds
    val rows = regionIds.map(regionData)

    val allMonuments = monumentDb.monuments.size
    val picturedMonuments = (totalImageDb.map(_.ids).getOrElse(Set.empty) ++ monumentDb.picturedIds).size

    val ids = stat.mapYears(_.ids)

    val photoSize = stat.mapYears(_.images.size)

    val idsSize = ids.map(_.size)

    val totalByYear = idsSize.zip(photoSize).flatMap { case (ids, photos) => Seq(ids, photos) }

    val totalData = Seq(
      "Total",
      allMonuments.toString,
      picturedMonuments.toString,
      (if (allMonuments != 0) 100 * picturedMonuments / allMonuments else 0).toString) ++ totalByYear.map(_.toString)

    Table(columns, rows ++ Seq(totalData), "Objects pictured")
  }

  def regionalStatImages(): Some[String] = {
    val dataset = new DefaultCategoryDataset()

    stat.monumentDb.get.regionIds.foreach { regionId =>
      val picturedIds = stat.mapYears(_.idsByRegion(regionId).size)
      val regionName = contest.country.regionName(regionId)

      val shortRegionName = regionName.replaceAll("область", "").replaceAll("Автономна Республіка", "АР")
      picturedIds.zipWithIndex.foreach { case (n, i) => dataset.addValue(n, yearSeq(i), shortRegionName) }
    }

    val ids = stat.mapYears(_.ids)
    val idsSize = ids.map(_.size)

    val categoryName = contest.imagesCategory
    val filenamePrefix = contest.name.replace("_", "")

    Some(regionalStatImages(filenamePrefix, categoryName, yearSeq, dataset, ids, idsSize, uploadImages))
  }

  def regionalStatImages(
                          filenamePrefix: String,
                          categoryName: String,
                          yearSeq: Seq[Int],
                          dataset: DefaultCategoryDataset,
                          ids: Seq[Set[String]],
                          idsSize: Seq[Int],
                          uploadImages: Boolean = true): String = {
    val images =
      s"\n[[File:${filenamePrefix}PicturedByYearTotal.png|$categoryName, monuments pictured by year overall|left]]" +
        s"\n[[File:${filenamePrefix}PicturedByYearPie.png|$categoryName, monuments pictured by year pie chart|left]]" +
        s"\n[[File:${filenamePrefix}PicturedByYear.png|$categoryName, monuments pictured by year by regions|left]]" +
        "\n<br clear=\"all\">"

    if (uploadImages) {
      val charts = new Charts()

      val chart = charts.createChart(dataset, "Регіон")
      val byRegionFile = filenamePrefix + "PicturedByYear"
      charts.saveCharts(chart, byRegionFile, 900, 1200)
      MwBot.fromHost(MwBot.commons).page(byRegionFile + ".png").upload(byRegionFile + ".png")

      val chartTotal = charts.createChart(charts.createTotalDataset(yearSeq, idsSize), "")

      val chartTotalFile = filenamePrefix + "PicturedByYearTotal.png"
      charts.saveAsPNG(chartTotal, chartTotalFile, 900, 200)
      MwBot.fromHost(MwBot.commons).page(chartTotalFile).upload(chartTotalFile)

      val intersectionFile = filenamePrefix + "PicturedByYearPie"
      charts.intersectionDiagram("Унікальність фотографій пам'яток за роками", intersectionFile, yearSeq, ids, 900, 800)
      MwBot.fromHost(MwBot.commons).page(intersectionFile + ".png").upload(intersectionFile + ".png")
    }
    images
  }
}
