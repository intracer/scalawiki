package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.ImageDB

class AuthorMonuments(val stat: ContestStat,
                      newObjectRating: Option[Int] = None,
                      gallery: Boolean = false,
                      commons: Option[MwBot] = None,
                      val newAuthorObjectRating: Option[Int] = None) extends Reporter {

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
    allIds.size + newObjectRating.fold(0) {
      rating => (allIds -- oldIds).size * (rating - 1)
    } + newAuthorObjectRating.fold(0) {
      rating => ((allIds intersect oldIds) -- oldAuthorIds).size * (rating - 1)
    }

  def rowData(ids: Set[String], images: Int,
              regionRating: String => Int,
              userOpt: Option[String] = None): Seq[String] = {

    val objects = if (gallery && userOpt.isDefined && ids.nonEmpty) {
      userGalleryLink(ids, userOpt)
    } else {
      ids.size
    }

    val ratingColumns = if (newObjectRating.isDefined) {
      val oldAuthorIds = userOpt.map(oldImageDb.idByAuthor).getOrElse(Set.empty)
      Seq(
        (ids intersect oldIds intersect oldAuthorIds).size, // existing
        (ids intersect oldIds -- oldAuthorIds).size, // new for author
        (ids -- oldIds).size, // new
        ratingFunc(ids, oldIds, oldAuthorIds) // rating
      )
    } else Seq.empty[String]

    val byRegion = country.regionIds.toSeq.map(regionRating)

    ((objects +: ratingColumns :+ images) ++ byRegion).map(_.toString)
  }

  private def userGalleryLink(ids: Set[String], userOpt: Option[String]) = {
    val noTemplateUser = userOpt.get.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")

    val galleryPage = "Commons:" + contest.name + "/" + noTemplateUser

    val galleryText = new Output().galleryByRegionAndId(imageDb.monumentDb.get, imageDb.subSet(_.author == userOpt), oldImageDb)

    commons.foreach(_.page(galleryPage).edit(galleryText))

    "[[" + galleryPage + "|" + ids.size + "]]"
  }

  override def table: Table = {

    val columns = Seq("User", "Objects pictured") ++
      (if (newObjectRating.isDefined) Seq("Existing", "New for author", "New", "Rating") else Seq.empty) ++
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
