package org.scalawiki.wlx.stat.reports

import org.jfree.data.category.DefaultCategoryDataset
import org.scalawiki.MwBot
import org.scalawiki.dto.Image
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.stat.ContestStat
import org.scalawiki.wlx.{ImageDB, MonumentDB}

class AuthorsStat(val uploadImages: Boolean = false) {

  var userImages: Option[String] = None

  val charts = new Charts()

  def authorsStat(data: ContestStat, bot: MwBot, gallery: Boolean) = {
    new AuthorMonuments(data, gallery = gallery, commons = Some(bot))
      .updateWiki(bot)
  }

  def authorsContributed(
      imageDbs: Seq[ImageDB],
      totalImageDb: Option[ImageDB],
      monumentDb: Option[MonumentDB]
  ): String = {

    val table = authorsContributedTable(imageDbs, totalImageDb, monumentDb)

    val header = "\n==Authors contributed==\n"
    header + table.asWiki + userImages.getOrElse("")
  }

  def authorsContributedPerRegion(imageDb: ImageDB, bot: MwBot) = {
    imageDb.monumentDb.map { monumentDb =>
      monumentDb.regionIds.map { regionId =>
        val country = monumentDb.contest.country
        val regionName = country.regionName(regionId)
        val authors =
          imageDb
            .authorsByRegion(regionId)
            .toSeq
            .sorted
            .map(_.replace("Участник:", ""))
            .filterNot(_.isBlank)
        val listPage =
          "Commons:Wiki Loves Monuments in Ukraine/Автори " + regionName
        val text = authors
          .map(author => s"# {{#target:User_talk:$author|}}")
          .mkString("\n")
        bot.page(listPage).edit(text)
      }
    }
  }

  def authorsContributedTable(
      imageDbs: Seq[ImageDB],
      totalImageDb: Option[ImageDB],
      monumentDb: Option[MonumentDB],
      listAuthors: Boolean = true
  ): Table = {
    val contest = monumentDb.map(_.contest).getOrElse(imageDbs.head.contest)
    val categoryName = contest.imagesCategory

    val imageDbsByYear = imageDbs.groupBy(_.contest.year)
    val yearSeq = imageDbsByYear.keys.toSeq.sorted.reverse

    val numYears = yearSeq.size

    val dataset = new DefaultCategoryDataset()

    val yearDbs = yearSeq.flatMap { year =>
      imageDbsByYear(year).headOption
    }
    val dbs = totalImageDb.toSeq ++ yearDbs

    val columns = Seq("Region") ++
      totalImageDb.map(_ => s"$numYears years total").toSeq ++
      yearSeq.map(_.toString)

    val perRegion = monumentDb.fold(Seq.empty[Seq[String]]) { db =>
      val country = db.contest.country
      db.regionIds.map { regionId =>
        val regionName = country.regionName(regionId)

        val shortRegionName = regionName
          .replaceAll("область", "")
          .replaceAll("Автономна Республіка", "АР")

        yearDbs.zipWithIndex.foreach { case (yearDb, i) =>
          dataset.addValue(
            yearDb.authorsByRegion(regionId).size,
            yearSeq(i),
            shortRegionName
          )
        }

        Seq(regionName) ++
          totalImageDb.map { db =>
            val count = db.authorsByRegion(regionId).size.toString
            if (listAuthors) {
              val listPage =
                "Commons:Wiki Loves Monuments in Ukraine/Автори " + regionName
              s"[[$listPage|$count]]"
            } else count
          } ++
          yearDbs.map(_.authorsByRegion(regionId).size.toString)
      }
    }

    val totalData = Seq("Total") ++ dbs.map(_.authors.size.toString)

    val rows = perRegion ++ Seq(totalData)

    val filenamePrefix = contest.name.replace("_", "")

    val ids = yearSeq.map(year => imageDbsByYear(year).head.authors)
    val idsSize = ids.map(_.size)

    userImages = Some(
      authorsStatImages(
        filenamePrefix,
        categoryName,
        yearSeq,
        dataset,
        ids,
        idsSize,
        uploadImages
      )
    )

    new Table(columns, rows, "Authors contributed")
  }

  def authorsImages(
      byAuthor: Map[String, Seq[Image]],
      monumentDb: Option[MonumentDB]
  ): String = {

    val sections = byAuthor
      .mapValues(images =>
        monumentDb.fold(images)(db =>
          images.filter(_.monumentId.fold(false)(db.ids.contains))
        )
      )
      .collect {
        case (user, images) if images.nonEmpty =>
          val userLink = s"[[User:$user|$user]]"
          val header = s"== $userLink ==\n"
          val descriptions = images
            .map(i => i.mpx + " МПкс (" + i.resolution.getOrElse("") + ")")

          header + Image.gallery(images.map(_.title), descriptions)
      }

    sections.mkString("__TOC__\n", "\n", "")
  }

  def authorsStatImages(
      filenamePrefix: String,
      categoryName: String,
      yearSeq: Seq[Int],
      dataset: DefaultCategoryDataset,
      ids: Seq[Set[String]],
      idsSize: Seq[Int],
      uploadImages: Boolean = true
  ) = {

    val images =
      s"\n[[File:${filenamePrefix}AuthorsByYearTotal.png|$categoryName, Authors by year overall|left]]" +
        s"\n[[File:${filenamePrefix}AuthorsByYearPie.png|$categoryName, Authors by year pie chart|left]]" +
        s"\n[[File:${filenamePrefix}AuthorsByYear.png|$categoryName, Authors by year by regions|left]]" +
        "\n<br clear=\"all\">"

    if (uploadImages) {

      val chart = charts.createChart(dataset, "Регіон")
      val byRegionFile = filenamePrefix + "AuthorsByYear"
      charts.saveCharts(chart, byRegionFile, 900, 1200)
      MwBot
        .fromHost(MwBot.commons)
        .page(byRegionFile + ".png")
        .upload(byRegionFile + ".png")

      val chartTotal =
        charts.createChart(charts.createTotalDataset(yearSeq, idsSize), "")

      val chartTotalFile = filenamePrefix + "AuthorsByYearTotal.png"
      charts.saveAsPNG(chartTotal, chartTotalFile, 900, 200)
      MwBot.fromHost(MwBot.commons).page(chartTotalFile).upload(chartTotalFile)

      val intersectionFile = filenamePrefix + "AuthorsByYearPie"
      charts.intersectionDiagram(
        "Унікальність авторів за роками",
        intersectionFile,
        yearSeq,
        ids,
        900,
        800
      )
      MwBot
        .fromHost(MwBot.commons)
        .page(intersectionFile + ".png")
        .upload(intersectionFile + ".png")
    }
    images
  }
}
