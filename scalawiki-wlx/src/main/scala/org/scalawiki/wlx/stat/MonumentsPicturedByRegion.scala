package org.scalawiki.wlx.stat
import org.jfree.data.category.DefaultCategoryDataset
import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.{ImageDB, MonumentDB}

class MonumentsPicturedByRegion(val stat: ContestStat, uploadImages: Boolean = false)  extends Reporter {

  def this(imageDbs: Seq[ImageDB], totalImageDb: Option[ImageDB], monumentDb: MonumentDB) = {
    this(new ContestStat(
      monumentDb.contest,
      imageDbs.headOption.map(_.contest.year).getOrElse(monumentDb.contest.year),
      Some(monumentDb),
      imageDbs.lastOption.orElse(totalImageDb),
      totalImageDb,
      imageDbs//.headOption.map(_ => imageDbs.init).getOrElse(Seq.empty)
    ))
  }

  val name = "Monuments pictured by region"

  var images: Option[String] = None
  val charts = new Charts()

  override def asText: String = {

    val header = s"\n==$name==\n"

    header + table.asWiki + images.getOrElse("")
  }

  def table =
    monumentsPicturedTable(stat.dbsByYear, stat.totalImageDb, stat.monumentDb.get)

  def monumentsPicturedTable(imageDbs: Seq[ImageDB], totalImageDb: Option[ImageDB], monumentDb: MonumentDB): Table = {
    val contest = monumentDb.contest
    val categoryName = contest.imagesCategory
    val filenamePrefix = contest.name.replace("_", "")

    val imageDbsByYear = imageDbs.groupBy(_.contest.year)
    val yearSeq = imageDbsByYear.keys.toSeq.sorted
    val numYears = yearSeq.size

    val yearsColumns = yearSeq.flatMap {
      year =>
        val ys = year.toString
        Seq(ys + " Objects", ys + " Pictures")
    }

    val columns = Seq("Region", "Objects in lists",
      s"$numYears years total", s"$numYears years percentage") ++ yearsColumns

    val dataset = new DefaultCategoryDataset()

    val regionIds = monumentDb.regionIds

    val withPhotoInLists = monumentDb.monuments.filter(_.photo.isDefined).map(_.id).toSet

    val rows = regionIds.map { regionId =>

      val withPhotoInListsCurrentRegion = withPhotoInLists.filter(id => Monument.getRegionId(id) == regionId)
      val picturedMonumentsInRegionSet = totalImageDb.map(_.idsByRegion(regionId)).getOrElse(Set.empty) ++
        withPhotoInListsCurrentRegion
      val picturedMonumentsInRegion = picturedMonumentsInRegionSet.size
      val allMonumentsInRegion = monumentDb.byRegion(regionId).size

      val picturedIds = yearSeq.map {
        year =>
          val db = imageDbsByYear(year).head
          db.idsByRegion(regionId).size
      }
      val pictured = yearSeq.flatMap {
        year =>
          val db = imageDbsByYear(year).head
          Seq(
            db.idsByRegion(regionId).size,
            db.imagesByRegion(regionId).toSet.size
          )
      }

      val regionName = contest.country.regionName(regionId)
      val columnData = (Seq(
        regionName,
        allMonumentsInRegion,
        picturedMonumentsInRegion,
        100 * picturedMonumentsInRegion / allMonumentsInRegion) ++ pictured).map(_.toString)

      val shortRegionName = regionName.replaceAll("область", "").replaceAll("Автономна Республіка", "АР")
      picturedIds.zipWithIndex.foreach { case (n, i) => dataset.addValue(n, yearSeq(i), shortRegionName) }

      columnData
    }

    val allMonuments = monumentDb.monuments.size
    val picturedMonuments = (totalImageDb.map(_.ids).getOrElse(Set.empty) ++ withPhotoInLists).size

    val ids = yearSeq.map(year => imageDbsByYear(year).head.ids)

    val photoSize = yearSeq.map(year => imageDbsByYear(year).head.images.size)

    val idsSize = ids.map(_.size)

    val totalByYear = idsSize.zip(photoSize).flatMap { case (ids, photos) => Seq(ids, photos) }

    val totalData = Seq(
      "Total",
      allMonuments.toString,
      picturedMonuments.toString,
      (if (allMonuments != 0) 100 * picturedMonuments / allMonuments else 0).toString) ++ totalByYear.map(_.toString)

    images = Some(regionalStatImages(filenamePrefix, categoryName, yearSeq, dataset, ids, idsSize, uploadImages))

    new Table(columns, rows ++ Seq(totalData), "Objects pictured")
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
