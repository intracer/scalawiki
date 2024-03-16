package org.scalawiki.wlx.stat.rating

import org.scalawiki.wlx.stat.ContestStat

case class NewlyPicturedPerAuthorBonus(
    stat: ContestStat,
    newlyPicturedRate: Double,
    newlyPicturedPerAuthorRate: Double
) extends Rater {

  val oldMonumentIdsByAuthor: Map[String, Set[String]] = oldImages
    .groupBy(_.author.getOrElse(""))
    .mapValues(_.flatMap(_.monumentId).toSet)
    .toMap

  override def rate(monumentId: String, author: String): Double = {
    val rate = monumentId match {
      case id if disqualify(id, author) => 0
      case id if !oldMonumentIds.contains(id) =>
        newlyPicturedRate - 1
      case id
          if !oldMonumentIdsByAuthor
            .getOrElse(author, Set.empty)
            .contains(id) =>
        newlyPicturedPerAuthorRate - 1
      case _ =>
        0
    }
    rate
  }

  override def explain(monumentId: String, author: String): String = {
    monumentId match {
      case id if disqualify(id, author) =>
        "Disqualified for reuploading similar images = 0"
      case id if !oldMonumentIds.contains(id) =>
        s"Newly pictured bonus = ${newlyPicturedRate - 1}"
      case id
          if !oldMonumentIdsByAuthor
            .getOrElse(author, Set.empty)
            .contains(id) =>
        s"Newly pictured per author bonus = ${newlyPicturedPerAuthorRate - 1}"
      case _ =>
        s"Not newly pictured = 0"
    }
  }

  override def disqualify(monumentId: String, author: String): Boolean = {
    Set("Петро Халява", "SnizhokAM").contains(author) &&
    oldMonumentIdsByAuthor.getOrElse(author, Set.empty).contains(monumentId)
  }
}
