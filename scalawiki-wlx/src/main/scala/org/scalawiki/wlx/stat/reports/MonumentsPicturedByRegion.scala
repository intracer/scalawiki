package org.scalawiki.wlx.stat.reports

import org.jfree.data.category.DefaultCategoryDataset
import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto.AdmDivision
import org.scalawiki.wlx.stat.ContestStat
import org.scalawiki.wlx.{ImageDB, MonumentDB}

class MonumentsPicturedByRegion(
    val stat: ContestStat,
    uploadImages: Boolean = false,
    regionParam: Option[AdmDivision] = None,
    gallery: Boolean = false
) extends Reporter {

  def this(
      imageDbs: Seq[ImageDB],
      totalImageDb: ImageDB,
      monumentDb: MonumentDB
  ) = this(
    ContestStat(
      monumentDb.contest,
      imageDbs.headOption
        .map(_.contest.year)
        .getOrElse(monumentDb.contest.year),
      Some(monumentDb),
      imageDbs.lastOption.getOrElse(totalImageDb),
      totalImageDb,
      imageDbs
    )
  )

  val yearSeq: Seq[Int] = stat.yearSeq.reverse
  val regionalDetails: Boolean = stat.config.exists(_.regionalDetails)
  val parentRegion: AdmDivision = regionParam.getOrElse(stat.contest.country)
  val monumentDb: MonumentDB = stat.monumentDb.get
  val bot: MwBot = MwBot.fromHost(MwBot.commons)
  val name: String = pageName(parentRegion.name)

  val currentYearPageIds: Set[Long] =
    stat.currentYearImageDb.images.flatMap(_.pageId).toSet
  val oldImagesMonumentIds: Set[String] = stat.totalImageDb.images
    .filter(image => !currentYearPageIds.contains(image.pageId.get))
    .flatMap(_.monumentIds)
    .toSet

  def pageName(region: String, newly: Boolean = false): String = {
    if (newly) s"Monuments newly pictured by region in ${region}"
    else s"Monuments pictured by region in ${region}"
  }

  override def asText: String = {
    val header = s"\n==$name==\n"
    header + table.asWiki + regionalStatImages().getOrElse("") + wrongRegionIds
  }

  def table: Table = monumentsPicturedTable(stat.totalImageDb, monumentDb)

  def columns: Seq[String] = {
    val numYears = yearSeq.size
    val yearsColumns = yearSeq.flatMap { year =>
      val ys = year.toString
      Seq(s"$ys Objects", s"$ys Pictures") ++
        (if (year == yearSeq.head) Seq(s"$ys newly pictured") else Nil)
    }

    (if (parentRegion == contest.country) {
       Seq(
         "Region",
         "Objects in lists",
         s"$numYears years total",
         s"$numYears years percentage"
       )
     } else {
       Seq("Region (KOATUU)", "Objects in lists", "Total", s"Total percentage")
     }) ++ yearsColumns
  }

  def regionData(
      regionId: String,
      regionalDetails: Boolean = regionalDetails,
      totalImageDb: ImageDB,
      monumentDb: MonumentDB
  ): Seq[String] = {
    val picturedMonumentsInRegionSet = totalImageDb.idsByRegion(regionId) ++
      monumentDb.picturedInRegion(regionId)
    val picturedMonumentsInRegion = picturedMonumentsInRegionSet.size
    val allMonumentsInRegion = monumentDb.byRegion(regionId).size

    val regionName =
      if (parentRegion.code != regionId) parentRegion.regionName(regionId)
      else parentRegion.name
    val percentage =
      if (allMonumentsInRegion != 0)
        100 * picturedMonumentsInRegion / allMonumentsInRegion
      else 0

    val newIds = stat.currentYearImageDb.idsByRegion(regionId) -- oldImagesMonumentIds
    val newlyPicturedPage = s"Commons:$category/${pageName(regionName, true)}"

    val pictured = stat
      .mapYears { db =>
        Seq(
          db.idsByRegion(regionId).size,
          db.imagesByRegion(regionId).toSet.size
        ) ++
          (if (db.contest.year == yearSeq.head) {
             if (newIds.nonEmpty)
               Seq(s"[[$newlyPicturedPage|${newIds.size}]]")
             else
               Seq("0")
           } else Nil)
      }
      .reverse
      .flatten

    val newImagesDb = stat.currentYearImageDb.subSet(_.monumentIds.exists(newIds.contains))

    if (gallery) {
      bot
        .page(newlyPicturedPage)
        .edit(Output.galleryByMonumentId(newImagesDb, monumentDb))
    }

    val columnData = (Seq(
      if (regionalDetails && regionId.length == 2)
        s"[[Commons:$category/${pageName(regionName)}|$regionName ($regionId)]]"
      else if (regionId.length != 2) s"$regionName ($regionId)"
      else regionName,
      allMonumentsInRegion,
      picturedMonumentsInRegion,
      percentage
    ) ++ pictured).map(_.toString)

    if (regionId.length == 2 && regionalDetails) {
      val child = new MonumentsPicturedByRegion(
        stat,
        false,
        parentRegion.byMonumentId(regionId),
        gallery
      )
      child.updateWiki(bot)
    }

    columnData
  }

  def monumentsPicturedTable(
      totalImageDb: ImageDB,
      monumentDb: MonumentDB
  ): Table = {
    val regionIds = parentRegion.regions.map(_.code)
    val rows = regionIds
      .map(regionData(_, regionalDetails, totalImageDb, monumentDb))
      .filter(row => regionalDetails || row(1) != "0")

    val allMonuments = monumentDb.monuments.size
    val picturedMonuments = (totalImageDb.ids ++ monumentDb.picturedIds).size

    val ids = stat.mapYears(_.ids).reverse
    val photoSize = stat.mapYears(_.images.size).reverse

    val totalByYear =
      ids.zip(photoSize).zipWithIndex.flatMap { case ((ids, photos), index) =>
        Seq(ids.size, photos) ++ (if (index == 0)
                                    Seq(
                                      (ids -- oldImagesMonumentIds).size
                                    )
                                  else Nil)
      }

    val totalData = if (parentRegion == contest.country) {
      Seq(
        Seq(
          "Total",
          allMonuments.toString,
          picturedMonuments.toString,
          (if (allMonuments != 0) 100 * picturedMonuments / allMonuments
           else 0).toString
        ) ++ totalByYear.map(_.toString)
      )
    } else {
      Seq(
        Seq("Sum") ++ rows.head.indices.tail.map(i => rows.map(_(i).toInt).sum.toString),
        regionData(parentRegion.code, false, totalImageDb, monumentDb)
      )
    }

    Table(columns, rows ++ totalData, "Objects pictured")
  }

  def wrongRegionIds: String = {
    val parentIds = monumentDb.byRegion(parentRegion.code).map(_.id).toSet
    val regionIds = parentRegion.regions
      .map(_.code)
      .flatMap(monumentDb.byRegion)
      .map(_.id)
      .toSet
    val ids = (parentIds diff regionIds).toSeq.sorted
    if (ids.nonEmpty) {
      s"\n== Wrong region ids ==\n${ids.mkString(", ")}"
    } else ""
  }

  def regionalStatImages(): Option[String] = {
    if (parentRegion == contest.country) {
      val dataset = new DefaultCategoryDataset()

      monumentDb.regionIds.foreach { regionId =>
        val picturedIds = stat.mapYears(_.idsByRegion(regionId).size)
        val regionName = contest.country.regionName(regionId)

        val shortRegionName = regionName
          .replaceAll("область", "")
          .replaceAll("Автономна Республіка", "АР")
        picturedIds.zipWithIndex.foreach { case (n, i) =>
          dataset.addValue(n, yearSeq(i), shortRegionName)
        }
      }

      val ids = stat.mapYears(_.ids)
      val idsSize = ids.map(_.size)

      val categoryName = contest.imagesCategory
      val filenamePrefix = contest.name.replace("_", "")

      Some(
        regionalStatImages(
          filenamePrefix,
          categoryName,
          yearSeq,
          dataset,
          ids,
          idsSize,
          uploadImages
        )
      )
    } else None
  }

  def regionalStatImages(
      filenamePrefix: String,
      categoryName: String,
      yearSeq: Seq[Int],
      dataset: DefaultCategoryDataset,
      ids: Seq[Set[String]],
      idsSize: Seq[Int],
      uploadImages: Boolean = false
  ): String = {
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

      val chartTotal =
        charts.createChart(charts.createTotalDataset(yearSeq, idsSize), "")

      val chartTotalFile = filenamePrefix + "PicturedByYearTotal.png"
      charts.saveAsPNG(chartTotal, chartTotalFile, 900, 200)
      bot.page(chartTotalFile).upload(chartTotalFile)

      val intersectionFile = filenamePrefix + "PicturedByYearPie"
      charts.intersectionDiagram(
        "Унікальність фотографій пам'яток за роками",
        intersectionFile,
        yearSeq,
        ids,
        900,
        800
      )
      bot.page(intersectionFile + ".png").upload(intersectionFile + ".png")
    }
    images
  }
}
