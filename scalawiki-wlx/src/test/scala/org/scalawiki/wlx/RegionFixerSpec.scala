package org.scalawiki.wlx

import org.scalawiki.util.TestUtils.resourceAsString
import org.scalawiki.wlx.dto.Contest
import org.specs2.mutable.Specification

class RegionFixerSpec extends Specification {
  val contest: Contest = Contest.WLMUkraine(2019)
  val country = contest.country
  val listConfig = contest.uploadConfigs.head.listConfig


  "fixer" should {

    "get all regions" in {
      val updater = new RegionFixerUpdater(new MonumentDB(contest, Nil))
      updater.raions.size === 490
    }

    "fix region 1" in {
      val wiki = resourceAsString("/org/scalawiki/wlx/region_to_fix.wiki")

      val parser = new WlxTemplateParser(listConfig, "Вікіпедія:Вікі любить пам'ятки/Житомирська область/Новоград-Волинський район")
      val monuments = parser.parse(wiki).toSeq
      val updater = new RegionFixerUpdater(new MonumentDB(contest, monuments))

      val canBeFixed = monuments.filter(updater.needsUpdate)

      canBeFixed.size === 95
    }

    "fix region 2" in {
      val wiki = resourceAsString("/org/scalawiki/wlx/region_to_fix_2.wiki")

      val parser = new WlxTemplateParser(listConfig, "Вікіпедія:Вікі любить пам'ятки/Донецька область/Мангушський район")
      val monuments = parser.parse(wiki).toSeq
      val updater = new RegionFixerUpdater(new MonumentDB(contest, monuments))

      val canBeFixed = monuments.filter(updater.needsUpdate)

      canBeFixed.size === 6
    }
  }
}
