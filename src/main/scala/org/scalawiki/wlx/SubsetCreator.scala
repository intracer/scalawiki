package org.scalawiki.wlx

import org.scalawiki.wlx.dto.{Contest, Monument}
import org.scalawiki.wlx.query.MonumentQuery
import org.scalawiki.MwBot

import scala.collection.immutable.SortedSet

object SubsetCreator {

  import scala.concurrent.ExecutionContext.Implicits.global


  def main(args: Array[String]) {
    val ukWiki = MwBot.get("uk.wikipedia.org")

    val specialNomination = "національного значення"

    val contest = Contest.WLMUkraine(2014, "", "")
    val query = MonumentQuery.create(contest)
    query.byMonumentTemplateAsync(contest.listTemplate).map {
      monuments =>

        val subset = monuments.filter { m =>
          val typ = m.typ.getOrElse("").toLowerCase
          Set("нац" ).exists(typ.contains)
        }

        val byRegion = subset.groupBy(m => Monument.getRegionId(m.id))

        val regionIds = SortedSet(byRegion.keys.toSeq: _*)

        val regionOnPage = false

        try {
          regionPerPage(ukWiki, specialNomination, contest, byRegion, regionIds)
//          regionsOnPage(ukWiki, specialNomination, contest, byRegion, regionIds)
        } catch {
          case t:Throwable =>
            println(t)
            throw t
        }
    }

  }

  def regionsOnPage(ukWiki: MwBot, specialNomination: String, contest: Contest, byRegion: Map[String, Seq[Monument]], regionIds: SortedSet[String]) {
    val buf = new StringBuffer
    buf.append("__TOC__\n")

    for (regionId <- regionIds) {

      val regionTitle = contest.country.regionById.get(regionId).fold("-")(_.name)
      val regionLink = "Вікіпедія:Вікі любить пам'ятки/" + regionTitle

      buf.append(s"\n== $regionTitle ==\n")

      buf.append("{{WLM-шапка}}")
      val regionMonuments = byRegion(regionId)

      val byPage = regionMonuments.groupBy(_.page)
      val pages = SortedSet(byPage.keys.toSeq: _*)

      for (page <- pages) {
        val title = page.replace("Вікіпедія:Вікі любить пам'ятки/", "")
        buf.append(s"|-\n|colspan=9 bgcolor=lightyellow|\n=== [[$page|$title]] ===\n|-\n")
        byPage(page).foreach {
          monument =>
            val text = monument.text.split("\\|\\}")(0)
            buf.append(s"{{WLM-рядок$text")
        }
      }
      buf.append("\n|}")

    }

    val s = buf.toString

    //ukWiki.page(regionLink +" дерев'яна архітектура").edit(s, s"$regionTitle - дерев'яна архітектура")
    ukWiki.page(s"Вікіпедія:Вікі любить пам'ятки $specialNomination").edit(s, specialNomination)

  }

  def regionPerPage(ukWiki: MwBot, specialNomination: String, contest: Contest, byRegion: Map[String, Seq[Monument]], regionIds: SortedSet[String]) {
    for (regionId <- regionIds) {

      val regionTitle = contest.country.regionById.get(regionId).fold("-")(_.name)
      val regionLink = "Вікіпедія:Вікі любить пам'ятки/" + regionTitle

      val regionMonuments = byRegion(regionId)

      val byPage = regionMonuments.groupBy(_.page)
      val pages = SortedSet(byPage.keys.toSeq: _*)

      val buf = new StringBuffer
      buf.append(s"{{WLM $specialNomination}}\n__TOC__\n")
      buf.append("{{WLM-шапка}}")

      for (page <- pages) {
        val title = page.replace("Вікіпедія:Вікі любить пам'ятки/", "")
        buf.append(s"|-\n|colspan=9 bgcolor=lightyellow|\n=== [[$page|$title]] ===\n|-\n")
        byPage(page).foreach {
          monument =>
            val text = monument.text.split("\\|\\}")(0)
            buf.append(s"{{WLM-рядок$text")
        }
      }
      buf.append("\n|}")
      val s = buf.toString

      //ukWiki.page(regionLink +" дерев'яна архітектура").edit(s, s"$regionTitle - дерев'яна архітектура")
      ukWiki.page(s"$regionLink $specialNomination").edit(s, s"$regionTitle - $specialNomination")

    }
  }
}
