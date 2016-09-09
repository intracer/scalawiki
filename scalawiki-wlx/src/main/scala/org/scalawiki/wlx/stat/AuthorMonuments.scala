package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.ImageDB

class AuthorMonuments(imageDb: ImageDB,
                      rating: Boolean = false,
                      gallery: Boolean = false,
                      commons: Option[MwBot] = None) extends Reporter {

  override def stat: ContestStat = ???

  override def contest = imageDb.contest

  override def name: String = "Number of objects pictured by uploader"

  val country = contest.country

  val oldIds = imageDb.oldMonumentDb.fold(Set.empty[String])(_.withImages.map(_.id).toSet)

  def ratingFunc(allIds: Set[String], oldIds: Set[String]): Int =
    if (rating)
      allIds.size + (allIds -- oldIds).size
    else
      allIds.size

  def rowData(ids: Set[String], images: Int,
              regionRating: String => Int,
              rating: Boolean = false,
              user: Option[String] = None): Seq[String] = {

    val objects = if (gallery && user.isDefined && ids.nonEmpty) {

      val noTemplateUser = user.get.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")

      val galleryPage = "Commons:" + contest.name + "/" + noTemplateUser

      val galleryText = new Output().galleryByRegionAndId(imageDb.monumentDb.get, imageDb.subSet(_.author == user))

      commons.foreach(_.page(galleryPage).edit(galleryText))

      "[[" + galleryPage + "|" + ids.size + "]]"

    } else {
      ids.size
    }

    val ratingColumns = if (rating) {
      Seq(
        (ids intersect oldIds).size, // existing
        (ids -- oldIds).size, // new
        ratingFunc(ids, oldIds) // rating
      )
    } else Seq.empty[String]

    val byRegion = country.regionIds.toSeq.map(regionRating)

    ((objects +: ratingColumns :+ images) ++ byRegion).map(_.toString)
  }

  override def table: Table = {

    val columns = Seq("User", "Objects pictured") ++
      (if (rating) Seq("Existing", "New", "Rating") else Seq.empty) ++
      Seq("Photos uploaded") ++
      country.regionNames

    val totalData = "Total" +:
      rowData(imageDb.ids, imageDb.images.size, regId => imageDb.idsByRegion(regId).size, rating)

    val authors = imageDb.authors.toSeq.sortBy(user => -imageDb._byAuthorAndId.by(user).size)
    val authorsData = authors.map { user =>
      val noTemplateUser = user.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")
      val userLink = s"[[User:$noTemplateUser|$noTemplateUser]]"

      def userRating(regId: String) = {
        val monumentsInRegion = imageDb._byAuthorAndRegion.by(user, regId).flatMap(_.monumentId).toSet
        ratingFunc(monumentsInRegion, oldIds)
      }
      userLink +:
        rowData(imageDb._byAuthorAndId.by(user).keys, imageDb._byAuthor.by(user).size, userRating, rating, Some(user))
    }

    new Table(columns, totalData +: authorsData, name)
  }

}
