package org.scalawiki.wlx.stat

import org.jfree.data.category.DefaultCategoryDataset
import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto.AdmDivision
import org.scalawiki.wlx.{ImageDB, MonumentDB}

class MonumentsPicturedByRegion(val stat: ContestStat, uploadImages: Boolean = false, regionParam: Option[AdmDivision] = None) extends Reporter {

  def this(imageDbs: Seq[ImageDB], totalImageDb: Option[ImageDB], monumentDb: MonumentDB) = {
    this(ContestStat(
      monumentDb.contest,
      imageDbs.headOption.map(_.contest.year).getOrElse(monumentDb.contest.year),
      Some(monumentDb),
      imageDbs.lastOption.orElse(totalImageDb),
      totalImageDb,
      imageDbs
    ))
  }


  val yearSeq = stat.yearSeq.reverse
  val regionalDetails = stat.config.exists(_.regionalDetails)
  val parentRegion = regionParam.getOrElse(stat.contest.country)

  def pageName(region: String) = s"Monuments pictured by region in ${region}"
  val name = pageName(parentRegion.name)

  override def asText: String = {

    val header = s"\n==$name==\n"

    header + table.asWiki + regionalStatImages().getOrElse("")
  }

  def table = monumentsPicturedTable(stat.dbsByYear, stat.totalImageDb, stat.monumentDb.get)

  private val bot: MwBot = MwBot.fromHost(MwBot.commons)

  def monumentsPicturedTable(imageDbs: Seq[ImageDB], totalImageDb: Option[ImageDB], monumentDb: MonumentDB): Table = {

    val columns = {
      val numYears = yearSeq.size
      val yearsColumns = yearSeq.flatMap { year =>
        val ys = year.toString
        Seq(ys + " Objects", ys + " Pictures")
      }

      Seq("Region (KOATUU)", "Objects in lists", "Total", s"Total percentage") ++ yearsColumns
    }

    def regionData(regionId: String) = {
      val picturedMonumentsInRegionSet = totalImageDb.map(_.idsByRegion(regionId)).getOrElse(Set.empty) ++
        monumentDb.picturedInRegion(regionId)
      val picturedMonumentsInRegion = picturedMonumentsInRegionSet.size
      val allMonumentsInRegion = monumentDb.byRegion(regionId).size

      val pictured = stat.mapYears { db =>
        Seq(
          db.idsByRegion(regionId).size,
          db.imagesByRegion(regionId).toSet.size
        )
      }.reverse.flatten

      val regionName = parentRegion.regionName(regionId)
      val percentage = if (allMonumentsInRegion != 0)  100 * picturedMonumentsInRegion / allMonumentsInRegion else 0

      val columnData = (Seq(
        if (regionalDetails && regionId.length == 2) s"[[Commons:$category/${pageName(regionName)}|$regionName ($regionId)]]" else s"$regionName ($regionId)",
        allMonumentsInRegion,
        picturedMonumentsInRegion,
        percentage) ++ pictured).map(_.toString)

      if (regionId.length == 2  && stat.config.exists(_.regionalDetails)) {
        val child = new MonumentsPicturedByRegion(stat, false, parentRegion.byId(regionId))
        child.updateWiki(bot)
      }

      columnData
    }

    val regionIds = parentRegion.regions.map(_.code)
    val rows = regionIds.map(regionData)

    val allMonuments = monumentDb.monuments.size
    val picturedMonuments = (totalImageDb.map(_.ids).getOrElse(Set.empty) ++ monumentDb.picturedIds).size

    val ids = stat.mapYears(_.ids).reverse

    val photoSize = stat.mapYears(_.images.size).reverse

    val idsSize = ids.map(_.size)

    val totalByYear = idsSize.zip(photoSize).flatMap { case (ids, photos) => Seq(ids, photos) }

    val totalData = if (parentRegion == contest.country) Seq(Seq(
      "Total",
      allMonuments.toString,
      picturedMonuments.toString,
      (if (allMonuments != 0) 100 * picturedMonuments / allMonuments else 0).toString) ++ totalByYear.map(_.toString)
    ) else Nil

    Table(columns, rows ++ totalData, "Objects pictured")
  }

  def regionalStatImages(): Option[String] = {
    if (parentRegion == contest.country) {
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
    } else None
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
      bot.page(byRegionFile + ".png").upload(byRegionFile + ".png")

      val chartTotal = charts.createChart(charts.createTotalDataset(yearSeq, idsSize), "")

      val chartTotalFile = filenamePrefix + "PicturedByYearTotal.png"
      charts.saveAsPNG(chartTotal, chartTotalFile, 900, 200)
      bot.page(chartTotalFile).upload(chartTotalFile)

      val intersectionFile = filenamePrefix + "PicturedByYearPie"
      charts.intersectionDiagram("Унікальність фотографій пам'яток за роками", intersectionFile, yearSeq, ids, 900, 800)
      bot.page(intersectionFile + ".png").upload(intersectionFile + ".png")
    }
    images
  }
}
