package org.scalawiki.bots.stat

import java.nio.charset.StandardCharsets

import org.scalawiki.dto.filter.{AllRevisionsFilter, RevisionFilter}
import org.scalawiki.dto.history.History
import org.scalawiki.dto.{Page, Revision}

class RevisionStat(val page: Page,
                   val byRevisionSize: Map[Revision, Long],
                   byUserSize: Map[String, Long]) {

  val history = new History(page.revisions)

  val users = page.history.users(AllRevisionsFilter)
  val delta = page.history.delta(AllRevisionsFilter).getOrElse(0L)

  val addedOrRewritten = byRevisionSize.values.sum

  def byUserSize(user: String): Long = byUserSize.getOrElse(user, 0L)

  val usersSorted = byUserSize.toSeq.sortBy {
    case (user, size) => -size
  }

  val usersSortedString = usersSorted.map {
    case (user, size) => s"[[User:$user|$user]]: $size"
  }.mkString("<br>")

  def stat = s"| [[${page.title}]] || $addedOrRewritten || $delta || ${byUserSize.keySet.size} || $usersSortedString"

}

object RevisionStat {

  def statHeader = s"! title !! added or rewritten !! size increase !! users !! users by added or rewritten text size\n"

  def fromPage(page: Page, revFilter: RevisionFilter = AllRevisionsFilter) = {
    val annotation = new RevisionAnnotation(page, revFilter)

    val byRevisionSize = annotation.byRevisionContent.map {
      case (rev, words) =>
        rev.withoutContent -> words.map(_.getBytes(StandardCharsets.UTF_8).length.toLong).sum
    }

    val byUserSize = annotation.byUserContent.mapValues(_.map(_.getBytes(StandardCharsets.UTF_8).length.toLong).sum).toMap

    new RevisionStat(
      page.copy(revisions = revFilter(page.revisions.map(_.withoutContent))),
      byRevisionSize, byUserSize
    )
  }

}