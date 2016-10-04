package org.scalawiki.wlx.query

import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class WlxSql {

  val dc = DatabaseConfig.forConfig[JdbcProfile]("tool_labs")

  val db = dc.db

  import dc.driver.api._

  def getUserAndDate(category: String): DBIO[Seq[(String, String, Long)]] = {

    sql"""SELECT rev_user_text, rev_timestamp, rev_page
          FROM commonswiki_p.revision r
          JOIN commonswiki_p.categorylinks c
          ON r.rev_page = c.cl_from
          WHERE rev_parent_id = 0
          AND c.cl_to = "Images_from_Wiki_Loves_Monuments_2012_in_Ukraine"
    """.as[(String, String, Long)]
  }

}


object WlxSql {

  import scala.concurrent.ExecutionContext.Implicits.global

  def main(args: Array[String]) {

    val wlxSql = new WlxSql()

    try {
      val q = wlxSql.getUserAndDate("Images_from_Wiki_Loves_Monuments_2012_in_Ukraine")

      val f = wlxSql.db.run(q)
      val r = Await.result(f, Duration.Inf)

      println(r.size)

      r.headOption.foreach {
        case (user, time, pageId) =>
          println(s"$user, $time, $pageId")
      }

      //        .map {
      //        seq =>
      //          println(seq)
      //
      //        //        seq.foreach {
      //        //          user =>
      //        //          //case (user, time, pageId) =>
      //        //            println(s"$user, time, pageId")
      //        //        }
      //      }

    } finally {
      wlxSql.db.close()
    }
  }
}
