package org.scalawiki.wlx.stat

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import org.scalawiki.MwBot
import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.query.{ImageQuery, ImageQueryApi}
import org.scalawiki.wlx.{ImageDB, ListFiller, MonumentDB}

import scala.concurrent.Future
import scala.util.control.NonFatal

class Statistics(contest: Contest) {

  import scala.concurrent.ExecutionContext.Implicits.global

  val currentYear = contest.year

  val previousContests = (2013 until currentYear).map(year => Contest.WLEUkraine(year, "05-01", "05-31"))

  private val bot = MwBot.get(MwBot.commons)

  def init(): Unit = {

    val (monumentDb, monumentDbOld) = (Some(MonumentDB.getMonumentDb(contest)), None)

    imagesStatistics(contest, monumentDb, monumentDbOld)
  }

  def articleStatistics(monumentDb: MonumentDB) = {
    println(Stats.withArticles(monumentDb).asWiki("Article Statistics").asWiki)
  }

  def toMassMessage(users: Iterable[String]) = {
    users.map(name => s"{{#target:User talk:$name}}")
  }

  def imagesStatistics(contest: Contest, monumentDb: Option[MonumentDB], oldMonumentDb: Option[MonumentDB] = None) {
    val imageQuery = ImageQuery.create(db = false)

    val imageDbF = currentYear(contest, monumentDb, oldMonumentDb, imageQuery)
    val totalF = new ImageQueryApi().imagesWithTemplateAsync(contest.uploadConfigs.head.fileTemplate, contest)


    for (imageDb <- imageDbF;
         totalImages <- totalF) {

      val totalImageDb = new ImageDB(contest, totalImages, monumentDb)

      //photoWithoutArticle(monumentDb.get, totalImageDb)

      regionalStat(contest, monumentDb, imageQuery, imageDb, totalImageDb)

    }
  }

  def currentYear(contest: Contest, monumentDb: Option[MonumentDB], oldMonumentDb: Option[MonumentDB], imageQueryApi: ImageQuery) = {
    val imageDbFuture = ImageDB.create(contest, imageQueryApi, monumentDb, oldMonumentDb)

    imageDbFuture map {
      imageDb =>

        //new SpecialNominations().specialNominations(contest, imageDb)


        authorsStat(imageDb)
        //        byDayAndRegion(imageDb)
        //new SpecialNominations().specialNominations(contest, imageDb)
        lessThan2MpGallery(contest, imageDb)

        monumentDb.foreach { mDb =>
          wrongIds(contest, imageDb, mDb)

          fillLists(mDb, imageDb)
        }
        imageDb
    }
  }

  def message(bot: MwBot, user: String, msg: String => String): Unit = {
    bot.page("User_talk:" + user).edit(msg(user), section = Some("new"))
  }


  def lessThan2MpGallery(contest: Contest, imageDb: ImageDB) = {
    val lessThan2Mp = imageDb.byMegaPixelFilterAuthorMap(_ < 2)
    val gallery = new Output().authorsImages(lessThan2Mp, imageDb.monumentDb)
    val contestPage = s"${contest.contestType.name} ${contest.year} in ${contest.country.name}"

    bot.page(s"Commons:$contestPage/Less than 2Mp").edit(gallery, Some("updating"))
  }

  def wrongIds(wlmContest: Contest, imageDb: ImageDB, monumentDb: MonumentDB) {

    val wrongIdImages = imageDb.images.filterNot(image => image.monumentId.fold(false)(monumentDb.ids.contains))

    val contest = imageDb.contest
    val contestPage = s"${contest.contestType.name} ${contest.year} in ${contest.country.name}"

    val text = wrongIdImages.map(_.title).mkString("<gallery>", "\n", "</gallery>")
    bot.page(s"Commons:$contestPage/Images with bad ids").edit(text, Some("updating"))
  }

  def byDayAndRegion(imageDb: ImageDB): Unit = {
    //    val byDay = imageDb.withCorrectIds.groupBy(_.date.map(_.toDate))
    //
    //    val firstSlice = (16 to 16).flatMap(day => byDay.get)//.getOrElse(day.toString, Seq.empty))
    //
    //    val byRegion = firstSlice.groupBy(im => Monument.getRegionId(im.monumentId))
    //
    //    var text = ""
    //
    //    val contest = imageDb.contest
    //    val contestPage = s"${contest.contestType.name} ${contest.year} in ${contest.country.name}"
    //
    //    val dayPage = s"Commons:$contestPage/Day 2"
    //    for (regionId <- SortedSet(byRegion.keySet.toSeq: _*)) {
    //      val regionName: String = imageDb.monumentDb.contest.country.regionById(regionId).name
    //      val pageName = s"$dayPage Region $regionName"
    //      val gallery = byRegion(regionId).map(_.title).mkString("<gallery>\n", "\n", "</gallery>")
    //
    //      text += s"* [[$pageName|$regionName]]\n"
    //
    //      MwBot.get(MwBot.commons).page(pageName).edit(gallery, "updating")
    //    }
    //    MwBot.get(MwBot.commons).page(dayPage).edit(text, "updating")
  }

  def authorsStat(imageDb: ImageDB) {
    val contest = imageDb.contest
    val contestPage = s"${contest.contestType.name} ${contest.year} in ${contest.country.name}"

    val output = new Output()

    val numberOfMonuments = output.authorsMonuments(imageDb)
    Files.write(Paths.get("authorsMonuments.txt"), numberOfMonuments.getBytes(StandardCharsets.UTF_8))
    bot.page(s"Commons:$contestPage/Number of objects pictured by uploader")
      .edit(numberOfMonuments, Some("updating"))

    val rating = output.authorsMonuments(imageDb, rating = true)
    Files.write(Paths.get("authorsRating.txt"), rating.getBytes(StandardCharsets.UTF_8))
    bot.page(s"Commons:$contestPage/Rating based on number and originality of objects pictured by uploader")
      .edit(rating, Some("updating"))

  }

  def regionalStat(wlmContest: Contest,
                   monumentDb: Option[MonumentDB],
                   imageQueryDb: ImageQuery,
                   currentYear: ImageDB,
                   totalImageDb: ImageDB) {

    val contest = currentYear.contest
    val categoryName = contest.contestType.name + " in " + contest.country.name

    val dbsByYear = previousContests.map(contest => ImageDB.create(contest, imageQueryDb, monumentDb)) ++ Seq(Future.successful(currentYear))

    Future.sequence(dbsByYear).map {
      imageDbs =>

        try {

          val output = new Output()

          val idsStat = monumentDb.map(db => output.monumentsPictured(imageDbs, totalImageDb, db)).getOrElse("")

          val authorStat = output.authorsContributed(imageDbs, totalImageDb, monumentDb)

          val toc = "__TOC__"
          val category = s"\n[[Category:$categoryName]]"
          val regionalStat = toc + idsStat + authorStat + category

          bot.page(s"Commons:$categoryName/Regional statistics").edit(regionalStat, Some("updating"))

          val authorsByRegionTotal = output.authorsMonuments(totalImageDb) + s"\n[[Category:$categoryName]]"

          bot.page(s"Commons:$categoryName/Total number of objects pictured by uploader").edit(authorsByRegionTotal, Some("updating"))

          monumentDb.map { db =>
            val mostPopularMonuments = output.mostPopularMonuments(imageDbs, totalImageDb, db)
            bot.page(s"Commons:$categoryName/Most photographed objects").edit(mostPopularMonuments, Some("updating"))
          }

          }
          catch {
            case NonFatal(e) =>
              println(e)
              e.printStackTrace()
              throw e
          }
        }
    }

    def fillLists(monumentDb: MonumentDB, imageDb: ImageDB): Unit = {
      ListFiller.fillLists(monumentDb, imageDb)
    }

    def photoWithoutArticle(monumentDb: MonumentDB, imageDb: ImageDB): String = {

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

  }

  object Statistics {
    def main(args: Array[String]) {

      val stat = new Statistics(Contest.WLEUkraine(2016, "05-01", "05-31"))

      stat.init()

    }
  }
