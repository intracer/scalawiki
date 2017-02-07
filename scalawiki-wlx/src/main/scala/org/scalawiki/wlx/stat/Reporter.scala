package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table

trait Reporter {

  def stat: ContestStat

  def contest = stat.contest

  def name: String

  def category = contest.name

  def page = s"Commons:$category/$name"

  def table: Table

  def asText: String = {
    val header = s"\n==$name==\n"

    val categoryText = s"\n[[Category:$category]]"

    header + table.asWiki + categoryText
  }

  def updateWiki(bot: MwBot) = {
    bot.page(page).edit(asText, Some("updating"))
  }

}
