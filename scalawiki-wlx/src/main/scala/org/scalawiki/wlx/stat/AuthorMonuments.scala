package org.scalawiki.wlx.stat

import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.ImageDB

class AuthorMonuments(val imageDb: ImageDB, rating: Boolean = false, gallery: Boolean = false) extends Reporter {

  override def stat: ContestStat = ???

  override def contest = imageDb.contest

  override def name: String = "Total number of objects pictured by uploader"

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
    (if (gallery && user.isDefined) {
      val galleryPage = s"Commons:${contest.name}/${user.get}"
      Seq(s"[[$galleryPage|${ids.size}]]")
    } else {
      Seq(ids.size.toString)
    }) ++
      ((if (rating) {
        Seq(
          (ids intersect oldIds).size,
          (ids -- oldIds).size,
          ratingFunc(ids, oldIds)
        )
      } else Seq.empty[String]) ++
        Seq(images) ++
        country.regionIds.toSeq.map(regionRating)).map(_.toString)
  }

  override def table: Table = {

    val columns = Seq("User", "Objects pictured") ++
      (if (rating) Seq("Existing", "New", "Rating") else Seq.empty) ++
      Seq("Photos uploaded") ++
      country.regionNames

    val totalData = Seq("Total") ++
      rowData(imageDb.ids, imageDb.images.size, regId => imageDb.idsByRegion(regId).size, rating)

    val authors = imageDb.authors.toSeq.sortBy(user => -imageDb._byAuthorAndId.by(user).size)
    val authorsData = authors.map { user =>
      val noTemplateUser = user.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")
      val userLink = s"[[User:$noTemplateUser|$noTemplateUser]]"

      def userRating(regId: String) = {
        val monumnentsInRegion = imageDb._byAuthorAndRegion.by(user, regId).flatMap(_.monumentId).toSet
        ratingFunc(monumnentsInRegion, oldIds)
      }
      Seq(userLink) ++
        rowData(imageDb._byAuthorAndId.by(user).keys, imageDb._byAuthor.by(user).size, userRating, rating, Some(user))
    }

    new Table(columns, Seq(totalData) ++ authorsData, "Number of objects pictured by uploader")
  }

}
