package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.ImageDB

class AuthorMonuments(val stat: ContestStat,
                      gallery: Boolean = false,
                      commons: Option[MwBot] = None) extends Reporter {

  override def contest = stat.contest

  override def name: String = "Number of objects pictured by uploader"

  val country = contest.country

  val imageDb = stat.currentYearImageDb.get
  val currentImageIds = imageDb.images.flatMap(_.pageId).toSet

  val totalImageDb = stat.totalImageDb.get

  val oldImages = totalImageDb.images.filter(image => !currentImageIds.contains(image.pageId.get))

  val oldImageDb = new ImageDB(contest, oldImages, stat.monumentDb)

  val oldIds = oldImageDb.ids

  def ratingFunc(allIds: Set[String], oldIds: Set[String], oldAuthorIds: Set[String]): Int =
    allIds.size + contest.newObjectRating.fold(0) {
      rating => (allIds -- oldIds).size * (rating - 1)
    } + contest.newAuthorObjectRating.fold(0) {
      rating => ((allIds intersect oldIds) -- oldAuthorIds).size * (rating - 1)
    }

  def rowData(ids: Set[String], images: Int,
              regionRating: String => Int,
              userOpt: Option[String] = None): Seq[String] = {

    val objects = optionalUserGalleryLink(ids.size, userOpt)

    val ratingColumns = if (contest.newObjectRating.isDefined) {
      val oldAuthorIds = userOpt.map(oldImageDb.idByAuthor).getOrElse(Set.empty)
      Seq(
        (ids intersect oldIds intersect oldAuthorIds).size, // existing
        (ids intersect oldIds -- oldAuthorIds).size, // new for author
        (ids -- oldIds).size, // new
        ratingFunc(ids, oldIds, oldAuthorIds) // rating
      )
    } else Seq.empty[String]

    val byRegion = country.regionIds.toSeq.map{ regionId =>
      optionalUserGalleryLink(regionRating(regionId), userOpt, country.regionById.get(regionId).map(_.name))
    }

    ((objects +: ratingColumns :+ images) ++ byRegion).map(_.toString)
  }

  private def optionalUserGalleryLink(number: Int, userOpt: Option[String], regionOpt: Option[String] = None) = {
    if (gallery && userOpt.isDefined && number > 0) {
      userGalleryLink(number, userOpt, regionOpt)
    } else {
      number
    }
  }

  private def userGalleryLink(number: Int, userOpt: Option[String], regionOpt: Option[String] = None) = {
    val noTemplateUser = userOpt.get.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")

    val galleryPage = "Commons:" + contest.name + "/" + noTemplateUser + regionOpt.fold("") { region =>
      "#" + region.replaceAll(" ", "_")
    }

    val galleryText = new Output().galleryByRegionAndId(imageDb.monumentDb.get, imageDb.subSet(_.author == userOpt), oldImageDb)

    for (bot <- commons if regionOpt.isEmpty) {
      bot.page(galleryPage).edit(galleryText)
    }

    "[[" + galleryPage + "|" + number + "]]"
  }

  override def table: Table = {

    val columns = Seq("User", "Objects pictured") ++
      (if (contest.newObjectRating.isDefined) Seq("Existing", "New for author", "New", "Rating") else Seq.empty) ++
      Seq("Photos uploaded") ++
      country.regionNames

    val totalData = "Total" +:
      rowData(imageDb.ids, imageDb.images.size, regId => imageDb.idsByRegion(regId).size)

    val authors = imageDb.authors.toSeq.sortBy {
      user =>
        val rating = ratingFunc(allIds = imageDb.idByAuthor(user),
          oldIds,
          oldAuthorIds = oldImageDb.idByAuthor(user))
        (-rating, user)
    }

    val authorsData = authors.map { user =>
      val noTemplateUser = user.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")
      val userLink = s"[[User:$noTemplateUser|$noTemplateUser]]"

      def userRating(regId: String) = {
        val monumentsInRegion = imageDb._byAuthorAndRegion.by(user, regId).flatMap(_.monumentId).toSet
        val oldMonumentsInRegion = oldImageDb._byAuthorAndRegion.by(user, regId).flatMap(_.monumentId).toSet
        ratingFunc(monumentsInRegion, oldIds, oldMonumentsInRegion)
      }

      userLink +:
        rowData(imageDb._byAuthorAndId.by(user).keys, imageDb._byAuthor.by(user).size, userRating, Some(user))
    }

    new Table(columns, totalData +: authorsData, name)
  }

}