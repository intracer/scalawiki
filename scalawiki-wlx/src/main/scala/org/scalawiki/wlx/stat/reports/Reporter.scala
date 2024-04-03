package org.scalawiki.wlx.stat.reports

import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.stat.ContestStat

import scala.concurrent.Future

trait Reporter {

  def stat: ContestStat

  def contest: Contest = stat.contest

  def name: String

  def category: String = contest.name

  def page = s"Commons:$category/$name"

  def table: Table

  def asText: String = {
    val header = s"\n==$name==\n"

    val categoryText = s"\n[[Category:$category]]"

    header + table.asWiki + categoryText
  }

  def updateWiki(bot: MwBot): Future[Any] = {
    bot.page(page).edit(asText, Some("updating"))
  }

}
