package org.scalawiki.wlx.stat.reports
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.stat.ContestStat

import java.time.ZonedDateTime

class RecentlyTaken(val stat: ContestStat) extends Reporter {

  private val jun30 = ZonedDateTime.parse(s"${contest.year}-06-30T23:59:59Z")

  override def name: String = "RecentlyTaken"

  override def table: Table = {
    val data = stat.currentYearImageDb
      .map { db =>
        val images = db.images.filter { i =>
          i.metadata.exists(_.date.exists(_.isAfter(jun30))) &&
            !i.specialNominations.contains(s"WLM${contest.year}-UA-interior")
        }
        images.map { i =>
          List(
            s"[[:${i.title}]]",
            s"[[User:${i.author.getOrElse("")}|${i.author.getOrElse("")}]]",
            i.metadata.flatMap(_.date.map(_.toString)).getOrElse("")
          )

        }
      }
      .getOrElse(Nil)
    Table(Seq("photo", "author", "date"), data)
  }
}
