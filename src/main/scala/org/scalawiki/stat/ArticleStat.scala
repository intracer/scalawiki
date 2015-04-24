package org.scalawiki.stat

import org.joda.time.DateTime
import org.scalawiki.dto.Page
import org.scalawiki.dto.filter.RevisionFilter

class ArticleStat(val from: Option[DateTime], val to: Option[DateTime], val pages: Seq[Page], label: String) {

  val filter = new RevisionFilter(from, to)
  val revisionStats = pages.map(page => new RevisionStat(page, filter)).sortBy(-_.addedOrRewritten)

  val deltas = revisionStats.map(_.delta)
  val addedOrRewritten = revisionStats.map(_.addedOrRewritten)

  val added = new NumericArrayStat("Added", deltas)
  val addedOrRewrittenStat = new NumericArrayStat("Added or rewritten", addedOrRewritten)

  val userStat = new UserStat(revisionStats)

  //  def titlesAndNumbers(seq: Seq[(Page, Int)]) = seq.map { case (page, size) => s"[[${page.title}]] ($size)"}.mkString(", ")

  def pageStat = {
    val header = "{| class='wikitable sortable'\n" +
      "|+ pages\n" + RevisionStat.statHeader

    header + revisionStats.map(_.stat).mkString("\n|-\n", "\n|-\n", "") + "\n|}"
  }

  override def toString = {
    s"""
        |=== $label articles ===
        |* Number of articles: ${pages.size}
        |* Authors: ${userStat.users.size}
        |====  Bytes ====
        |* $addedOrRewrittenStat
        |* $added
        |""".stripMargin +
      "\n====  Page stat ====\n" + pageStat +
      "\n====  User stat ====\n" + userStat

  }
}
