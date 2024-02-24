package org.scalawiki.wlx.stat.reports

import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.ImageDB
import org.scalawiki.wlx.stat.ContestStat

class NewMonuments(val stat: ContestStat) extends Reporter {

  val sorted: Seq[ImageDB] = stat.dbsByYear.sortBy(_.contest.year)

  val ids: Seq[Set[String]] = sorted.map(_.ids)

  val oldsIds: Seq[Set[String]] =
    Seq(Set.empty[String]) ++ (1 until ids.size).map { i =>
      (0 until i).flatMap(ids).toSet
    }

  val newIds: Seq[Set[String]] = ids.indices.map { i => ids(i) -- oldsIds(i) }

  override def table: Table = {

    val columns = Seq("Year") ++ sorted.map(_.contest.year.toString)
    val idsRow = Seq("Ids") ++ ids.map(_.size.toString)
    val oldIdsRow = Seq("Old Ids") ++ oldsIds.map(_.size.toString)
    val newIdsRow = Seq("New Ids") ++ newIds.map(_.size.toString)
    new Table(columns, Seq(idsRow, oldIdsRow, newIdsRow))
  }

  override def name: String = "New Monuments"
}
