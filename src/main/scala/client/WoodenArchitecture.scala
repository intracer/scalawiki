package client

import client.wlx.dto.{Contest, Monument}
import client.wlx.query.MonumentQuery

import scala.collection.immutable.SortedSet

object WoodenArchitecture {

  import scala.concurrent.ExecutionContext.Implicits.global


  def main(args: Array[String]) {
    val ukWiki = MwBot.get("uk.wikipedia.org")

    val specialNomination = "замки і фортеці"

    val contest = Contest.WLMUkraine(2014, "", "")
    val query = MonumentQuery.create(contest)
    query.byMonumentTemplateAsync(contest.listTemplate).map {
      monuments =>

        val wooden = monuments.filter { m =>
          val name = m.name.toLowerCase
          Set("замок", "замк", "форт", " вал").exists(name.contains)
        }

        val byRegion = wooden.groupBy(m => Monument.getRegionId(m.id))

        val regionIds = SortedSet(byRegion.keys.toSeq: _*)

        for (regionId <- regionIds) {

          val regionTitle = contest.country.regionById(regionId).name
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
            byPage(page).foreach{
              monument =>
                val text = monument.textParam.split("\\|\\}")(0)
                buf.append(s"{{WLM-рядок${text}")
            }
          }
          buf.append("\n|}")
          val s = buf.toString

          //ukWiki.page(regionLink +" дерев'яна архітектура").edit(s, s"$regionTitle - дерев'яна архітектура")
          ukWiki.page(s"$regionLink $specialNomination").edit(s,  s"$regionTitle - $specialNomination")

        }
    }

  }

}
