package org.scalawiki.wlx.stat

import org.jfree.chart.JFreeChart
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.data.general.DefaultPieDataset
import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto._
import org.scalawiki.wlx.{ImageDB, MonumentDB}

import scala.collection.immutable.SortedSet
import scala.util.control.NonFatal

class Output {

  val charts = new Charts()

  def mostPopularMonuments(imageDbs: Seq[ImageDB], totalImageDb: ImageDB, monumentDb: MonumentDB) = {

    val imageDbsByYear = imageDbs.groupBy(_.contest.year)
    val contest = monumentDb.contest
    val categoryName = contest.contestType.name + " in " + contest.country.name

    val yearSeq = imageDbsByYear.keys.toSeq.sorted
    val numYears = yearSeq.size

    val columns = Seq("Id", "Name",
      s"$numYears years photos", s"$numYears years authors") ++
      yearSeq.flatMap(year => Seq(s"$year photos", s"$year authors"))

    val photosCountTotal = totalImageDb.imageCountById
    val authorsCountTotal = totalImageDb.authorsCountById

    val photoCounts = yearSeq.map(year => imageDbsByYear(year).head.imageCountById)
    val authorCounts = yearSeq.map(year => imageDbsByYear(year).head.authorsCountById)

    val counts = Seq(photosCountTotal, authorsCountTotal) ++ (0 to numYears - 1).flatMap(i => Seq(photoCounts(i), authorCounts(i)))

    val topPhotos = (Set(photosCountTotal) ++ photoCounts).flatMap(topN(12, _).toSet)
    val topAuthors = (Set(authorsCountTotal) ++ authorCounts).flatMap(topN(12, _).toSet)

    val allTop = topPhotos ++ topAuthors
    val allTopOrdered = allTop.toSeq.sorted


    val rows = allTopOrdered.map { id =>
      val monument = monumentDb.byId(id).get
      Seq(
        id,
        monument.name.replaceAll("\\[\\[", "[[:uk:") + monument.galleryLink
      ) ++ counts.map(_.getOrElse(id, 0).toString)
    }

    val table = new Table(columns, rows, "Most photographed objects")

    val header = "\n==Most photographed objects==\n"
    val category = s"\n[[Category:$categoryName]]"

    header + table.asWiki + category
  }

  def topN(n: Int, stat: Map[String, Int]) = stat.toSeq.sortBy(-_._2).take(n).map(_._1)

  def monumentsPictured(imageDbs: Seq[ImageDB], totalImageDb: ImageDB, monumentDb: MonumentDB) = {

    try {

      val contest = monumentDb.contest
      val filenamePrefix = contest.contestType.name.split(" ").mkString + "In" + contest.country.name
      val categoryName = contest.contestType.name + " in " + contest.country.name

      val imageDbsByYear = imageDbs.groupBy(_.contest.year)
      val yearSeq = imageDbsByYear.keys.toSeq.sorted
      val numYears = yearSeq.size

      val columns = Seq("Region", "Objects in lists",
        s"$numYears years total", s"$numYears years percentage") ++
        yearSeq.map(_.toString)

      val dataset = new DefaultCategoryDataset()

      val header = "\n==Objects pictured==\n"

      val regionIds = SortedSet(monumentDb._byRegion.keySet.toSeq: _*).filter(monumentDb.contest.country.regionIds.contains)

      val withPhotoInLists = monumentDb.monuments.filter(_.photo.isDefined).map(_.id).toSet

      var withPhotoInListsFromRegions = Set.empty[String]

      val ids = yearSeq.map(year => imageDbsByYear(year).head.ids)
      val idsSize = ids.map(_.size)

      val rows = regionIds.map { regionId =>

        val withPhotoInListsCurrentRegion = withPhotoInLists.filter(id => Monument.getRegionId(id) == regionId)
        val picturedMonumentsInRegionSet = (totalImageDb.idsByRegion(regionId) ++ withPhotoInListsCurrentRegion).toSet
        val picturedMonumentsInRegion = picturedMonumentsInRegionSet.size
        val allMonumentsInRegion: Int = monumentDb.byRegion(regionId).size

        val pictured = yearSeq.map(year => imageDbsByYear(year).head.idsByRegion(regionId).toSet.size)

        val regionName = monumentDb.contest.country.regionById(regionId).name
        val columnData = (Seq(
          regionName,
          allMonumentsInRegion,
          picturedMonumentsInRegion,
          100 * picturedMonumentsInRegion / allMonumentsInRegion) ++ pictured).map(_.toString)

        val shortRegionName = regionName.replaceAll("область", "").replaceAll("Автономна Республіка", "АР")
        pictured.zipWithIndex.foreach { case (n, i) => dataset.addValue(n, yearSeq(i), shortRegionName) }

        withPhotoInListsFromRegions ++= picturedMonumentsInRegionSet

        columnData
      }

      val allMonuments = monumentDb.monuments.size
      val picturedMonuments = (totalImageDb.ids ++ withPhotoInLists).size
      val totalData = Seq(
        "Total",
        allMonuments,
        picturedMonuments,
        100 * picturedMonuments / allMonuments) ++ idsSize

      val total = totalData.mkString("|-\n| ", " || ", "\n|}") +
        s"\n[[File:${filenamePrefix}PicturedByYearTotal.png|$categoryName, monuments pictured by year overall|left]]" +
        s"\n[[File:${filenamePrefix}PicturedByYearPie.png|$categoryName, monuments pictured by year pie chart|left]]" +
        s"\n[[File:${filenamePrefix}PicturedByYear.png|$categoryName, monuments pictured by year by regions|left]]" +
        "\n<br clear=\"all\">"

      val chart = charts.createChart(dataset, "Регіон")
      val byRegionFile = filenamePrefix + "PicturedByYear"
      saveCharts(charts, chart, byRegionFile, 900, 1200)
      MwBot.get(MwBot.commons).page(byRegionFile + ".png").upload(byRegionFile + ".png")

      val chartTotal = charts.createChart(charts.createTotalDataset(yearSeq, idsSize), "")

      val chartTotalFile = filenamePrefix + "PicturedByYearTotal.png"
      charts.saveAsPNG(chartTotal, chartTotalFile, 900, 200)
      MwBot.get(MwBot.commons).page(chartTotalFile).upload(chartTotalFile)

      val intersectionFile = filenamePrefix + "PicturedByYearPie"
      intersectionDiagram(charts, "Унікальність фотографій пам'яток за роками", intersectionFile, yearSeq, ids, 900, 800)
      MwBot.get(MwBot.commons).page(intersectionFile + ".png").upload(intersectionFile + ".png")

      val table = new Table(columns, rows, "Objects pictured")

      header + table.asWiki + total
    } catch {
      case NonFatal(e) =>
        println(e)
        e.printStackTrace()
        throw e
    }
  }

  // up to 3 years
  def intersectionDiagram(charts: Charts, title: String, filename: String, years: Seq[Int], idsSeq: Seq[Set[String]], width: Int, height: Int) {
    val intersection = idsSeq.reduce(_ intersect _)
    val union = idsSeq.reduce(_ ++ _)

    val sliding = idsSeq.sliding(2).toSeq ++ Seq(Seq(idsSeq.head, idsSeq.last))
    val idsNear = sliding.map(_.reduce((a, b) => a intersect b) -- intersection)

    val only = idsSeq.zipWithIndex.map { case (ids, i) => ids -- removeByIndex(idsSeq, i).reduce(_ ++ _) }

    val pieDataset = new DefaultPieDataset()

    // TODO map years

    pieDataset.setValue("2012", only(0).size)
    pieDataset.setValue("2012 & 2013", idsNear(0).size)
    pieDataset.setValue("2013", only(1).size)
    pieDataset.setValue("2013 & 2014", idsNear(1).size)
    pieDataset.setValue("2014", only(2).size)
    pieDataset.setValue("2012 & 2014", idsNear(2).size)
    pieDataset.setValue("2012 & 2013 & 2014", intersection.size)

    val pieChart = charts.createPieChart(pieDataset, title)
    saveCharts(charts, pieChart, filename, width, height)
  }

  def removeByIndex[T](seq: Seq[T], i: Int): Seq[T] = seq.take(i) ++ seq.drop(i + 1)

  def saveCharts(charts: Charts, chart: JFreeChart, name: String, width: Int, height: Int) {
    //charts.saveAsJPEG(chart, name + ".jpg", width, height)
    charts.saveAsPNG(chart, name + ".png", width, height)
    //charts.saveAsSVG(chart, name + ".svg", width, height)
  }

  def monumentsByType(/*imageDbs: Seq[ImageDB], totalImageDb: ImageDB,*/ monumentDb: MonumentDB) = {
    val regions = monumentDb.contest.country.regionById

    for ((typ, size) <- monumentDb._byType.mapValues(_.size).toSeq.sortBy(-_._2)) {
      val byRegion = monumentDb._byTypeAndRegion(typ)


      val regionStat = byRegion.toSeq.sortBy(-_._2.size).map {
        case (regionId, monuments) =>
          val byReg1 = s"${regions(regionId)}: ${monuments.size}"

          val byReg2 = if (byRegion.size == 1) {
            val byReg2Stat = monuments.groupBy(m => m.id.substring(0, 6))

            byReg2Stat.toSeq.sortBy(-_._2.size).map {
              case (regionId2, monuments2) =>
                s"$regionId2: ${monuments2.size}"
            }.mkString("(", ", ", ")")
          } else ""

          byReg1 + byReg2
      }.mkString(", ")
      println(s"$typ: ${monumentDb._byType(typ).size}, $regionStat")
    }
  }


  def authorsContributed(imageDbs: Seq[ImageDB], totalImageDb: ImageDB, monumentDb: MonumentDB) = {

    val imageDbsByYear = imageDbs.groupBy(_.contest.year)
    val yearSeq = imageDbsByYear.keys.toSeq.sorted
    val numYears = yearSeq.size

    val columns = Seq("Region", s"$numYears years total") ++ yearSeq.map(_.toString)

    val header = "\n==Authors contributed==\n"

    val totalData = (Seq(
      "Total",
      totalImageDb.authors.size) ++ yearSeq.map(year => imageDbsByYear(year).head.authors.size)).map(_.toString)

    val regionIds = SortedSet(monumentDb._byRegion.keySet.toSeq: _*)

    val rows =
      regionIds.map { regionId =>
        (Seq(
          monumentDb.contest.country.regionById(regionId).name,
          totalImageDb.authorsByRegion(regionId).size) ++
          yearSeq.map(year => imageDbsByYear(year).head.authorsByRegion(regionId).size)).map(_.toString)
      } ++ Seq(totalData)

    val authors = yearSeq.map(year => imageDbsByYear(year).head.authors)

    val contest = monumentDb.contest
    val filename = contest.contestType.name.split(" ").mkString + "In" + contest.country.name + "AuthorsByYearPie"
    intersectionDiagram(charts, "Унікальність авторів за роками", filename, yearSeq, authors, 900, 900)

    val table = new Table(columns, rows, "Authors contributed")

    header + table.asWiki
  }

  def authorsMonuments(imageDb: ImageDB) = {

    val contest = imageDb.contest
    val country = contest.country
    val columns = Seq("User", "Objects pictured", "Photos uploaded") ++ country.regionNames

    val header = "{| class='wikitable sortable'\n" +
      "|+ Number of objects pictured by uploader\n" +
      columns.mkString("!", "!!", "\n")

    var text = ""
    val totalData = Seq(
      "Total",
      imageDb.ids.size,
      imageDb.images.size
    ) ++ country.regionIds.toSeq.map(regId => imageDb.idsByRegion(regId).size)

    text += totalData.mkString("|-\n| ", " || ", "\n")

    val authors = imageDb.authors.toSeq.sortBy(user => -imageDb._authorsIds(user).size)
    for (user <- authors) {
      val columnData = Seq(
        user.replaceAll("\\{\\{", "").replaceAll("\\}\\}", ""),
        imageDb._authorsIds(user).size,
        imageDb._byAuthor(user).size
      ) ++ country.regionIds.toSeq.map(regId => imageDb._authorIdsByRegion(user).getOrElse(regId, Seq.empty).size)

      text += columnData.mkString("|-\n| ", " || ", "\n")
    }

    val total = "|}" + s"\n[[Category:${contest.contestType.name} ${contest.year} in ${country.name}]]"

    header + text + total
  }

  def authorsImages(byAuthor: Map[String, Seq[Image]], monumentDb: MonumentDB) = {

    val sections = byAuthor
      .mapValues(images => images.filter(_.monumentId.fold(false)(monumentDb.ids.contains)))
      .collect {
      case (user, images) if images.nonEmpty =>
        val userLink = s"[[User:$user|$user]]"
        val header = s"== $userLink =="
        val gallery = images.map {
          i =>
            val w = i.width.get
            val h = i.height.get
            val mp = w * h / Math.pow(10, 6)
            f"${i.title}| $mp%1.2f ${i.width.get} x ${i.height.get}"
        }

        header + gallery.mkString("\n<gallery>\n", "\n", "\n</gallery>")
    }

    sections.mkString("__TOC__\n", "\n", "")
  }


}
