package org.scalawiki.wlx.stat

import org.jfree.data.category.DefaultCategoryDataset
import org.scalawiki.MwBot
import org.scalawiki.dto.Image
import org.scalawiki.dto.markup.Table
import org.scalawiki.wikitext.TableParser
import org.scalawiki.wlx.dto._
import org.scalawiki.wlx.{ImageDB, MonumentDB}

import scala.util.control.NonFatal

class Output {

  val charts = new Charts()

  def mostPopularMonuments(imageDbs: Seq[ImageDB], totalImageDb: ImageDB, monumentDb: MonumentDB) = {

    val table = mostPopularMonumentsTable(imageDbs, totalImageDb, monumentDb)

    val header = "\n==Most photographed objects==\n"

    val contest = monumentDb.contest
    val categoryName = contest.contestType.name + " in " + contest.country.name
    val category = s"\n[[Category:$categoryName]]"

    header + table.asWiki + category
  }

  def mostPopularMonumentsTable(imageDbs: Seq[ImageDB], totalImageDb: ImageDB, monumentDb: MonumentDB): Table = {
    val imageDbsByYear = imageDbs.groupBy(_.contest.year)

    val yearSeq = imageDbsByYear.keys.toSeq.sorted
    val numYears = yearSeq.size

    val columns = Seq("Id", "Name",
      s"$numYears years photos", s"$numYears years authors") ++
      yearSeq.flatMap(year => Seq(s"$year photos", s"$year authors"))

    val photosCountTotal = totalImageDb.imageCountById
    val authorsCountTotal = totalImageDb.authorsCountById

    val photoCounts = yearSeq.map(year => imageDbsByYear(year).head.imageCountById)
    val authorCounts = yearSeq.map(year => imageDbsByYear(year).head.authorsCountById)

    val counts = Seq(photosCountTotal, authorsCountTotal) ++ (0 until numYears).flatMap(i => Seq(photoCounts(i), authorCounts(i)))

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

    new Table(columns, rows, "Most photographed objects")
  }

  def topN(n: Int, stat: Map[String, Int]) = stat.toSeq.sortBy(-_._2).take(n).map(_._1)

  def monumentsPictured(imageDbs: Seq[ImageDB], totalImageDb: ImageDB, monumentDb: MonumentDB) = {

    try {
      val header = "\n==Objects pictured==\n"

      val (images: String, table: Table) = monumentsPicturedTable(imageDbs, totalImageDb, monumentDb, uploadImages = true)

      header + table.asWiki + images
    }

    catch {
      case NonFatal(e) =>
        println(e)
        e.printStackTrace()
        throw e
    }
  }

  def monumentsPicturedTable(imageDbs: Seq[ImageDB], totalImageDb: ImageDB, monumentDb: MonumentDB, uploadImages: Boolean = false): (String, Table) = {
    val contest = monumentDb.contest
    val filenamePrefix = contest.contestType.name.split(" ").mkString + "In" + contest.country.name
    val categoryName = contest.contestType.name + " in " + contest.country.name

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

    val regionIds = monumentDb._byRegion.keySet.intersect(monumentDb.contest.country.regionIds).toSeq.sortBy(identity)

    val withPhotoInLists = monumentDb.monuments.filter(_.photo.isDefined).map(_.id).toSet

    val rows = regionIds.map { regionId =>

      val withPhotoInListsCurrentRegion = withPhotoInLists.filter(id => Monument.getRegionId(id) == regionId)
      val picturedMonumentsInRegionSet = (totalImageDb.idsByRegion(regionId) ++ withPhotoInListsCurrentRegion).toSet
      val picturedMonumentsInRegion = picturedMonumentsInRegionSet.size
      val allMonumentsInRegion = monumentDb.byRegion(regionId).size

      val picturedIds = yearSeq.map {
        year =>
          val db = imageDbsByYear(year).head
          db.idsByRegion(regionId).toSet.size
      }
      val pictured = yearSeq.flatMap {
        year =>
          val db = imageDbsByYear(year).head
          Seq(
            db.idsByRegion(regionId).toSet.size,
            db.imagesByRegion(regionId).toSet.size
          )
      }

      val regionName = monumentDb.contest.country.regionById(regionId).name
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
    val picturedMonuments = (totalImageDb.ids ++ withPhotoInLists).size


    val ids = yearSeq.map(year => imageDbsByYear(year).head.ids)

    val photoSize = yearSeq.map(year => imageDbsByYear(year).head.images.size)

    val idsSize = ids.map(_.size)

    val totalByYear = idsSize.zip(photoSize).flatMap { case (ids, photos) => Seq(ids, photos) }

    val totalData = Seq(
      "Total",
      allMonuments.toString,
      picturedMonuments.toString,
      (if (allMonuments != 0) 100 * picturedMonuments / allMonuments else 0).toString) ++ totalByYear.map(_.toString)

    val images = regionalStatImages(filenamePrefix, categoryName, yearSeq, dataset, ids, idsSize, uploadImages)

    val table = new Table(columns, rows ++ Seq(totalData), "Objects pictured")
    (images, table)
  }

  def regionalStatImages(
                          filenamePrefix: String,
                          categoryName: String,
                          yearSeq: Seq[Int],
                          dataset: DefaultCategoryDataset,
                          ids: Seq[Set[String]],
                          idsSize: Seq[Int],
                          uploadImages: Boolean = false): String = {
    val images =
      s"\n[[File:${filenamePrefix}PicturedByYearTotal.png|$categoryName, monuments pictured by year overall|left]]" +
        s"\n[[File:${filenamePrefix}PicturedByYearPie.png|$categoryName, monuments pictured by year pie chart|left]]" +
        s"\n[[File:${filenamePrefix}PicturedByYear.png|$categoryName, monuments pictured by year by regions|left]]" +
        "\n<br clear=\"all\">"

    if (uploadImages) {

      val chart = charts.createChart(dataset, "Регіон")
      val byRegionFile = filenamePrefix + "PicturedByYear"
      charts.saveCharts(chart, byRegionFile, 900, 1200)
      MwBot.get(MwBot.commons).page(byRegionFile + ".png").upload(byRegionFile + ".png")

      val chartTotal = charts.createChart(charts.createTotalDataset(yearSeq, idsSize), "")

      val chartTotalFile = filenamePrefix + "PicturedByYearTotal.png"
      charts.saveAsPNG(chartTotal, chartTotalFile, 900, 200)
      MwBot.get(MwBot.commons).page(chartTotalFile).upload(chartTotalFile)

      val intersectionFile = filenamePrefix + "PicturedByYearPie"
      charts.intersectionDiagram("Унікальність фотографій пам'яток за роками", intersectionFile, yearSeq, ids, 900, 800)
      MwBot.get(MwBot.commons).page(intersectionFile + ".png").upload(intersectionFile + ".png")
    }
    images
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
        val regionIds = db._byRegion.keySet.toSeq.filter(contest.country.regionIds.contains).sortBy(identity)
        regionIds.map {
          regionId =>
            (Seq(
              contest.country.regionById(regionId).name,
              totalImageDb.authorsByRegion(regionId).size) ++
              yearSeq.map(year => imageDbsByYear(year).head.authorsByRegion(regionId).size)).map(_.toString)
        }
    }

    val rows = perRegion ++ Seq(totalData)

    //    val authors = yearSeq.map(year => imageDbsByYear(year).head.authors)
    //    val filename = contest.contestType.name.split(" ").mkString + "In" + contest.country.name + "AuthorsByYearPie"
    //    charts.intersectionDiagram("Унікальність авторів за роками", filename, yearSeq, authors, 900, 900)

    new Table(columns, rows, "Authors contributed")
  }

  def authorsMonuments(imageDb: ImageDB, rating: Boolean = false) = {

    val contest = imageDb.contest
    val country = contest.country
    val columns = Seq("User") ++
      (if (rating) Seq("Objects pictured", "Existing", "New", "Rating")
      else Seq("Objects pictured")) ++
      Seq("Photos uploaded") ++ country.regionNames

    val oldIds = imageDb.oldMonumentDb.fold(Set.empty[String])(_.withImages.map(_.id).toSet)

    val header = "{| class='wikitable sortable'\n" +
      "|+ Number of objects pictured by uploader\n" +
      columns.mkString("!", "!!", "\n")

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

    var text = ""
    val totalData = Seq("Total") ++
      rowData(imageDb.ids, imageDb.images.size, regId => imageDb.idsByRegion(regId).size)

    text += totalData.mkString("|-\n| ", " || ", "\n")

    val authors = imageDb.authors.toSeq.sortBy(user => -imageDb._authorsIds(user).size)
    for (user <- authors) {
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
      val columnData = Seq(userLink) ++
        rowData(imageDb._authorsIds(user), imageDb._byAuthor(user).size, userRating)

      text += columnData.mkString("|-\n| ", " || ", "\n")
    }

    val total = "|}" + s"\n[[Category:${contest.contestType.name} ${contest.year} in ${country.name}]]"

    header + text + total
  }

  def authorsMonumentsTable(imageDb: ImageDB, rating: Boolean = false): Table = {
    val wiki = authorsMonuments(imageDb, rating)
    TableParser.parse(wiki)
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

  def galleryByRegionAndId(monumentDb: MonumentDB, imageDb: ImageDB) = {
    val country = monumentDb.contest.country
    val regionIds = country.regionIds.filter(id => imageDb.idsByRegion(id).nonEmpty)

    regionIds.map {
      regionId =>
        val regionName: String = country.regionById(regionId).name
        val regionHeader = s"== [[:uk:Вікіпедія:Вікі любить Землю/$regionName|$regionName]] ==\n"
        val ids = imageDb.idsByRegion(regionId)
        regionHeader + ids.map {
          id =>
            val images = imageDb.byId(id).map(_.title).sorted
            s"=== $id ===\n" +
              s"${monumentDb.byId(id).get.name}\n" +
              Image.gallery(images)
        }.mkString("\n")
    }.mkString("\n")
  }

}
