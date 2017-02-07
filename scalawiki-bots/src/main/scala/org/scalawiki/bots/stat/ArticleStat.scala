package org.scalawiki.bots.stat

import org.scalawiki.dto.filter.RevisionFilter

class ArticleStat(val filter: RevisionFilter, val revisionStats: Seq[RevisionStat], label: String) {

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
        |* Number of articles: ${revisionStats.size}
        |* Authors: ${userStat.users.size}
        |====  Bytes ====
        |* $addedOrRewrittenStat
        |* $added
        |""".stripMargin +
      "\n====  Page stat ====\n" + pageStat +
      "\n====  User stat ====\n" + userStat
  }
}
