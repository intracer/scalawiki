package org.scalawiki.wlx.stat.reports
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.stat.ContestStat

import java.nio.file.{Files, Path, Paths}
import java.time.ZonedDateTime
import scala.io.Codec

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
        // INSERT INTO selection (round_id, page_id, jury_id, rate)
        // VALUES (1, 2, 3, 4)
        //     , (4, 5, 6, 7)
        //     , (7, 8, 9, 10);
        val sql = "INSERT INTO selection (round_id, page_id, jury_id, rate) \n" +
          images
            .map { i =>
              s"(1347, ${i.pageId.get}, 2036, 0)"
            }
            .mkString("VALUES ", ",\n", ";")
        Files.write(Paths.get("RecentlyTaken.sql"), sql.getBytes(Codec.UTF8.charSet))

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
