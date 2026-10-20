package org.scalawiki.wlx.stat.reports

import org.jfree.data.category.DefaultCategoryDataset
import org.scalawiki.{ActionBot, MwBot}
import org.scalawiki.dto.{Image, User}
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.list.{ListParam, Users, UsUsers}
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.stat.ContestStat
import org.scalawiki.wlx.{ImageDB, MonumentDB}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthorsStat(val uploadImages: Boolean = false) {

  var userImages: Option[String] = None

  val charts = new Charts()

  def authorsStat(data: ContestStat, bot: MwBot, gallery: Boolean) = {
    new AuthorMonuments(data, gallery = gallery, commons = Some(bot))
      .updateWiki(bot)
  }

  def authorsContributed(
      imageDbs: Seq[ImageDB],
      totalImageDb: ImageDB,
      monumentDb: Option[MonumentDB]
  ): String = {

    val table = authorsContributedTable(imageDbs, totalImageDb, monumentDb)

    val header = "\n==Authors contributed==\n"
    header + table.asWiki + userImages.getOrElse("")
  }

  def authorsContributedPerRegion(imageDb: ImageDB, bot: MwBot): Future[Unit] =
    imageDb.monumentDb.fold(Future.unit) { monumentDb =>
      val authorsByRegion = monumentDb.regionIds.map { regionId =>
        val regionName = monumentDb.contest.country.regionName(regionId)
        val authors =
          imageDb
            .authorsByRegion(regionId)
            .toSeq
            .sorted
            .map(_.replace("Участник:", ""))
            .filterNot(_.isBlank)
        regionName -> authors
      }
      AuthorsStat.accounts(bot, authorsByRegion.flatMap(_._2).distinct).flatMap { accounts =>
        Future
          .traverse(authorsByRegion) { case (regionName, authors) =>
            bot.page(AuthorsStat.authorsListPage(imageDb.contest, regionName)).edit(AuthorsStat.authorsList(authors, accounts))
          }
          .map(_ => ())
      }
    }

  // a file whose page names no author groups under "", which is nobody to count
  private def named(authors: Set[String]): Set[String] = authors.filterNot(_.isBlank)

  def authorsContributedTable(
      imageDbs: Seq[ImageDB],
      totalImageDb: ImageDB,
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
    val dbs = Seq(totalImageDb) ++ yearDbs

    val columns = Seq("Region") ++
      Seq(s"$numYears years total") ++
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
            named(yearDb.authorsByRegion(regionId)).size,
            yearSeq(i),
            shortRegionName
          )
        }

        Seq(regionName) ++
          Seq {
            val count = named(totalImageDb.authorsByRegion(regionId)).size.toString
            if (listAuthors) {
              val listPage = AuthorsStat.authorsListPage(contest, regionName)
              s"[[$listPage|$count]]"
            } else count
          } ++
          yearDbs.map(yearDb => named(yearDb.authorsByRegion(regionId)).size.toString)
      }
    }

    val totalData = Seq("Total") ++ dbs.map(db => named(db.authors).size.toString)

    val rows = perRegion ++ Seq(totalData)

    val filenamePrefix = contest.name.replace("_", "")

    val ids = yearSeq.map(year => named(imageDbsByYear(year).head.authors))
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
        monumentDb.fold(images)(db => images.filter(_.monumentId.fold(false)(db.ids.contains)))
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
  ): String = {

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

object AuthorsStat {

  /** The page listing a region's all-time authors. It has no year in its title: Regional statistics links its
    * all-time author count here, so writing to a per-year title left the linked page stale.
    */
  def authorsListPage(contest: Contest, regionName: String): String =
    s"Commons:${contest.contestType.name} in ${contest.country.name}/Автори $regionName"

  /** The authors list, a MassMessage target list. An author with no Commons account (a Panoramio or Flickr name, a
    * name written out) is listed as plain text: it is still counted, but there is no talk page to message.
    */
  def authorsList(authors: Seq[String], accounts: Set[String]): String =
    authors
      .map(author => if (accounts.contains(userName(author))) s"# {{#target:User_talk:$author|}}" else s"# $author")
      .mkString("\n")

  /** Which of `authors` are Commons accounts, by their [[userName]]: one request per 50 authors, one at a time. */
  def accounts(bot: ActionBot, authors: Seq[String]): Future[Set[String]] =
    authors.map(userName).distinct.grouped(50).foldLeft(Future.successful(Set.empty[String])) { (acc, names) =>
      acc.flatMap { found =>
        bot.run(Action(Query(ListParam(Users(UsUsers(names)))))).map { pages =>
          // a missing user comes back without an id, and so does an invalid name
          found ++ pages.flatMap(_.lastRevisionUser).collect { case user: User if user.id.isDefined => user.login }.flatten
        }
      }
    }

  /** The name as MediaWiki stores it: spaces for underscores, the first letter capitalized. */
  def userName(author: String): String = author.replace('_', ' ').trim.capitalize
}
