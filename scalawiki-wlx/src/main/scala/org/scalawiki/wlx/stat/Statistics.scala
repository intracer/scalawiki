package org.scalawiki.wlx.stat

import java.time.{ZoneOffset, ZonedDateTime}

import org.scalawiki.MwBot
import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.query.{ImageQuery, MonumentQuery}
import org.scalawiki.wlx.{ImageDB, ListFiller, MonumentDB}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
  * Holds fetched contest data
  *
  * @param contest            contest: contest type (WLM/WLE), country, year, etc.
  * @param startYear          the first year contest was held, or the first year that we are interested in
  * @param monumentDb         cultural/natural monuments database for the contest
  * @param currentYearImageDb image database for current year's contest
  * @param totalImageDb       image database that holds images of all monuments from the contest, regardless of when they where uploaded
  * @param dbsByYear          image databases split by contest year
  * @param monumentDbOld      monument database at current year's contest start. Used to rate users who submitted newly pictured monuments.
  */
case class ContestStat(contest: Contest,
                       startYear: Int,
                       monumentDb: Option[MonumentDB],
                       currentYearImageDb: Option[ImageDB],
                       totalImageDb: Option[ImageDB],
                       dbsByYear: Seq[ImageDB] = Seq.empty,
                       monumentDbOld: Option[MonumentDB] = None)

/**
  * Coordinates fetching contest statistics and creating reports/galleries etc. Needs refactoring.
  *
  * @param contest       contest: contest type (WLM/WLE), country, year, etc.
  * @param startYear     the first year contest was held, or the first year that we are interested in
  * @param monumentQuery monuments fetcher
  * @param imageQuery    images fetcher
  * @param bot           scalawiki bot instance
  */
class Statistics(contest: Contest,
                 startYear: Option[Int],
                 monumentQuery: MonumentQuery,
                 imageQuery: ImageQuery,
                 bot: MwBot,
                 cfg: StatConfig) {

  def this(contest: Contest,
           startYear: Option[Int] = None,
           monumentQuery: MonumentQuery,
           imageQuery: ImageQuery = ImageQuery.create(),
           bot: MwBot = MwBot.fromHost(MwBot.commons),
           cfg: Option[StatConfig] = None) =
    this(contest, startYear, monumentQuery, imageQuery, bot, cfg.getOrElse(StatConfig(contest.campaign)))

  val currentYear = contest.year

  val contests = (startYear.getOrElse(currentYear) to currentYear).map(y => contest.copy(year = y))

  /**
    * Fetches contest data
    *
    * @param total whether to fetch image database that holds images of all monuments from the contest, regardless of when they where uploaded
    * @return asynchronously returned contest data
    */
  def gatherData(total: Boolean): Future[ContestStat] = {

    val (monumentDb, monumentDbOld) = (
      Some(MonumentDB.getMonumentDb(contest, monumentQuery)),
      Option(contest.newObjectRating).filter(_ == true).map { _ =>
        MonumentDB.getMonumentDb(contest, monumentQuery, date = Some(ZonedDateTime.of(2017, 4, 30, 23, 59, 0, 0, ZoneOffset.UTC)))
      }
    )

    for (byYear <- Future.sequence(contests.map(contestImages(monumentDb)));
         totalImages <- if (total) imagesByTemplate(monumentDb) else Future.successful(None)
    ) yield {
      val currentYearImages = byYear.find(_.contest.year == currentYear)

      val mDbOld: Option[MonumentDB] = currentYearImages.flatMap(getOldImagesMonumentDb(monumentDb, monumentDbOld, totalImages, _))

      ContestStat(contest, startYear.getOrElse(contest.year), monumentDb, currentYearImages, totalImages, byYear, mDbOld)
    }
  }

  private def contestImages(monumentDb: Some[MonumentDB])(contest: Contest) =
    ImageDB.create(contest, imageQuery, monumentDb)

  private def imagesByTemplate(monumentDb: Some[MonumentDB]) =
    imageQuery.imagesWithTemplateAsync(contest.uploadConfigs.head.fileTemplate, contest).map {
      images => Some(new ImageDB(contest, images, monumentDb))
    }

  def getOldImagesMonumentDb(monumentDb: Option[MonumentDB], monumentDbOld: Option[MonumentDB],
                             totalImages: Option[ImageDB], imageDB: ImageDB): Option[MonumentDB] = {
    for (mDb <- monumentDb;
         mdbOld <- monumentDbOld;
         total <- totalImages.orElse(Some(new ImageDB(contest, Seq.empty)))) yield {
      val oldIds = mdbOld.monuments.filter(_.photo.isDefined).map(_.id).toSet ++
        total.images.filterNot(ti => imageDB.images.exists(i => i.pageId.exists(ti.pageId.contains))).flatMap(_.monumentId)

      new MonumentDB(contest, mDb.monuments.filter(m => oldIds.contains(m.id)))
    }
  }

  def init(total: Boolean): Unit = {
    gatherData(total = total).map {
      data =>
        for (totalImageDb <- data.totalImageDb) {
          data.currentYearImageDb.foreach(imageDb => currentYear(data.contest, imageDb, data))
          regionalStat(data.contest, data.dbsByYear, totalImageDb, data)
        }
    }.failed.map(println)
  }

  def articleStatistics(monumentDb: MonumentDB) = {
    println(Stats.withArticles(monumentDb).asWiki("Article Statistics").asWiki)
  }

  def toMassMessage(users: Iterable[String]) = {
    users.map(name => s"{{#target:User talk:$name}}")
  }

  /**
    * Outputs current year reports.
    *
    * @param contest
    * @param imageDb
    * @param stat
    */
  def currentYear(contest: Contest, imageDb: ImageDB, stat: ContestStat) = {

    //new SpecialNominations(contest, imageDb).specialNominations()

    new AuthorsStat().authorsStat(imageDb, bot, stat.monumentDbOld)

    lessThan2MpGallery(contest, imageDb)

    imageDb.monumentDb.foreach {
      mDb =>
        wrongIds(imageDb, mDb)

      //fillLists(mDb, imageDb)
    }
  }

  def message(bot: MwBot, user: String, msg: String => String): Unit = {
    bot.page("User_talk:" + user).edit(msg(user), section = Some("new"))
  }

  def lessThan2MpGallery(contest: Contest, imageDb: ImageDB) = {
    val lessThan2Mp = imageDb.byMegaPixelFilterAuthorMap(_ < 2)
    val gallery = new AuthorsStat().authorsImages(lessThan2Mp, imageDb.monumentDb)
    val contestPage = contest.name

    bot.page(s"Commons:$contestPage/Less than 2Mp").edit(gallery, Some("updating"))
  }

  def wrongIds(imageDb: ImageDB, monumentDb: MonumentDB) {

    val wrongIdImages = imageDb.images.filterNot(image => image.monumentId.fold(false)(monumentDb.ids.contains))

    val contest = imageDb.contest
    val contestPage = contest.name

    val text = wrongIdImages.map(_.title).mkString("<gallery>", "\n", "</gallery>")
    bot.page(s"Commons:$contestPage/Images with bad ids").edit(text, Some("updating"))
  }

  def regionalStat(wlmContest: Contest,
                   imageDbs: Seq[ImageDB],
                   totalImageDb: ImageDB,
                   stat: ContestStat) {

    val contest = totalImageDb.contest
    val categoryName = contest.contestType.name + " in " + contest.country.name
    val monumentDb = totalImageDb.monumentDb

    val authorsStat = new AuthorsStat()

    val idsStat = monumentDb.map(db => new MonumentsPicturedByRegion(stat, uploadImages = true).asText).getOrElse("")

    val authorsContributed = authorsStat.authorsContributed(imageDbs, Some(totalImageDb), monumentDb)

    val toc = "__TOC__"
    val category = s"\n[[Category:$categoryName]]"
    val regionalStat = toc + idsStat + authorsContributed + category

    bot.page(s"Commons:$categoryName/Regional statistics").edit(regionalStat, Some("updating"))

    monumentDb.map(_ => new MostPopularMonuments(stat).updateWiki(bot))
  }

  def fillLists(monumentDb: MonumentDB, imageDb: ImageDB): Unit = {
    ListFiller.fillLists(monumentDb, imageDb)
  }

  def photoWithoutArticle(imageDb: ImageDB): String = {

    val monumentDb = imageDb.monumentDb.get

    val all = monumentDb.monuments.filter(m =>
      m.photo.isDefined &&
        m.article.isEmpty && !m.name.contains("[[") &&
        m.types.map(_.toLowerCase).exists(_.contains("нац")
          && imageDb.authorsCountById.getOrElse(m.id, 0) > 1
          && imageDb.byId(m.id).size > 2)
    )
    val byRegion = all.groupBy(_.regionId)

    val perRegion = monumentDb.contest.country.regions.sortBy(_.name).map {
      region =>
        val regionHeader = s"== ${region.name} ==\n"

        val monuments = byRegion.getOrElse(region.code, Seq.empty)

        val images = monuments.map(_.photo.get)
        val descriptions = monuments.map(m => s"[[${m.name}]], ${m.city.getOrElse("")}")

        val gallery = Image.gallery(images, descriptions)

        regionHeader + gallery
    }

    perRegion.mkString("\n")
  }

  def byCity(imageDb: ImageDB) = {

    val ukWiki = bot //MwBot.fromHost(MwBot.ukWiki)
    //    val cities = Seq("Бар (місто)", "Бершадь", "Гайсин", "Гнівань", "Жмеринка", "Іллінці",
    //      "Калинівка", "Козятин", "Ладижин", "Липовець", "Могилів-Подільський", "Немирів",
    //      "Погребище", "Тульчин", "Хмільник", "Шаргород", "Ямпіль")

    val cities = Seq("Баштанка",
      "Вознесенськ",
      "Нова Одеса",
      "Новий Буг",
      "Очаків",
      "Снігурівка",
      "Южноукраїнськ"
    )

    val monumentDb = imageDb.monumentDb.get

    val all = monumentDb.monuments.filter { m =>
      val city = m.city.getOrElse("").replaceAll("\\[", " ").replaceAll("\\]", " ")
      m.photo.isDefined && cities.map(_ + " ").exists(city.contains) && !city.contains("район") && Set("48").contains(m.regionId)
    }

    def cityShort(city: String) = cities.find(city.contains).getOrElse("").split(" ")(0)

    def page(city: String) = "User:Ilya/Миколаївська область/" + city

    all.groupBy(m => cityShort(m.city.getOrElse(""))).foreach {
      case (city, monuments) =>

        val galleries = monuments.map {
          m =>
            val images = imageDb.byId(m.id)
            val gallery = Image.gallery(images.map(_.title))

            s"""== ${m.name.replaceAll("\\[\\[", "[[:uk:")} ==
               |'''Рік:''' ${m.year.getOrElse("")}, '''Адреса:''' ${m.place.getOrElse("")}, '''Тип:''' ${m.typ.getOrElse("")},
               |'''Охоронний номер:''' ${m.stateId.getOrElse("")}\n""".stripMargin +
              gallery
        }
        ukWiki.page(page(city)).edit(galleries.mkString("\n"))
    }

    val list = cities.map(city => s"#[[${page(city)}|$city]]").mkString("\n")
    ukWiki.page("User:Ilya/Миколаївська область").edit(list)
  }

  def articleStatistics(monumentDb: MonumentDB, imageDb: ImageDB) = {
    val byRegionAndId = imageDb._byAuthorAndId
    for ((regId, byId) <- byRegionAndId.grouped) {
      val monuments = monumentDb.byRegion(regId).filter { m =>
        m.types.exists(t => t == "комплекс" || t.contains("нац")) &&
          m.photo.nonEmpty &&
          m.article.isEmpty
      }
      val toWrite = monuments.filter { m =>
        val id = m.id
        val images = byId.by(id)
        val authors = images.flatMap(_.author).toSet
        authors.size > 1 && images.size > 2
      }
      val gallery = toWrite.map {
        m =>
          m.photo.get + "| [[" + m.name + "]]" + m.city.fold("")(", " + _)
      }.mkString("<gallery>", "\n", "</gallery>")

      val region = toWrite.head.page.split("/")(1)

      val page = "Вікіпедія:Пам'ятки національного значення із фото і без статей/" + region

      MwBot.fromHost(MwBot.ukWiki).page(page).edit(gallery)
    }
  }

  def byRegionDnabb(imageDb: ImageDB): Unit = {

    val monumentDb = imageDb.monumentDb.get

    val all = monumentDb.monuments.filter(m =>
      m.photo.isDefined &&
        Set("53").contains(m.regionId)
    )
    val byRegion = all.groupBy(_.regionId)

    monumentDb.contest.country.regions.sortBy(_.name).foreach {
      region =>
        val regionHeader = s"== ${region.name} ==\n"

        val monumentSlices = byRegion.getOrElse(region.code, Seq.empty).sliding(100, 100).zipWithIndex

        for ((monuments, index) <- monumentSlices) {

          val images = monuments.map(_.photo.get)
          val descriptions = monuments.map(m => s"[[${m.name}]], ${m.city.getOrElse("")}")

          val gallery = Image.gallery(images, descriptions)

          val text = regionHeader + gallery

          val galleries = monuments.map {
            m =>
              val images = imageDb.byId(m.id)
              val gallery = Image.gallery(images.map(_.title))

              s"""== ${m.name.replaceAll("\\[\\[", "[[:uk:")} ==
                 |'''Рік:''' ${m.year.getOrElse("")}, '''Адреса:''' ${m.place.getOrElse("")}, '''Тип:''' ${m.typ.getOrElse("")},
                 |'''Охоронний номер:''' ${m.stateId.getOrElse("")}\n""".stripMargin + gallery
          }

          val contestTitle =
            imageDb.contest.contestType.name
          bot.page(s"$contestTitle - ${region.name} - ${index + 1}").edit(galleries.mkString("\n"))
        }
    }
  }

}


object Statistics {

  def main(args: Array[String]) {
    val cfg = StatParams.parse(args)

    val contest = Contest.byCampaign(cfg.campaign).get
      .copy(year = cfg.years.last, newObjectRating = cfg.newObjectRating)

    val stat = new Statistics(
      contest,
      startYear = Some(cfg.years.head),
      monumentQuery = MonumentQuery.create(contest),
      cfg = Some(cfg)
    )

    stat.init(total = cfg.years.size > 1)
  }
}
