package org.scalawiki.wlx

import org.scalawiki.util.TestUtils.resourceAsString
import org.scalawiki.wlx.dto.Contest
import org.specs2.mutable.Specification

class RegionFixerSpec extends Specification {
  val contest: Contest = Contest.WLMUkraine(2019)
  val country = contest.country
  val listConfig = contest.uploadConfigs.head.listConfig

  "fixer" should {
    "fix Novohrad-Volynskyy region" in {
      val wiki = resourceAsString("/org/scalawiki/wlx/region_to_fix.wiki")

      val parser = new WlxTemplateParser(listConfig, "Вікіпедія:Вікі любить пам'ятки/Житомирська область/Новоград-Волинський район")
      val monuments = parser.parse(wiki).toSeq

      val oblasts = country.regions.filter(adm => !Set("Київ", "Севастополь").contains(adm.name))
      val raions = oblasts.flatMap(_.regions).filter(_.name.endsWith("район"))
      raions.size === 490
      val raionNames = raions.map(_.name).toSet

      val highLevel = monuments.filter(m => raionNames.contains(m.cityName) && m.place.exists(_.trim.nonEmpty))
      println(s"highLevel size: ${highLevel.size}")

      val canBeFixed = highLevel.filter { m =>
        m.place.exists { p =>
          country.byIdAndName(m.regionId, p.split(",").head).size == 1
        }
      }

      canBeFixed.size === 95
    }
  }
}
