package org.scalawiki.wlx

import org.scalawiki.wlx.dto.{Contest, ContestType, Country}
import org.specs2.mutable.Specification

class ContestSpec extends Specification {

  "by campaign" should {
    "from config file" in {
      val c = Contest.byCampaign("wlm-ua").get
      c.country.code === "ua"
      c.country.name === "Ukraine"
      c.country === Country.Ukraine
      c.contestType === ContestType.WLM
      c.listTemplate === Some("ВЛП-рядок")
      c.fileTemplate === Some("Monument Ukraine")
      c.listsHost === Some("uk.wikipedia.org")

      c.uploadConfigs.size === 1
      val uc = c.uploadConfigs.head
      uc.campaign === "wlm-ua"
      uc.fileTemplate === "Monument Ukraine"
      uc.listsHost === None
      uc.listTemplate === "ВЛП-рядок"

      val lc = uc.listConfig
      lc.templateName === "ВЛП-рядок"
      lc.namesMap.keySet === Set(
        "ID",
        "name",
        "year",
        "city",
        "place",
        "lat",
        "lon",
        "stateId",
        "type",
        "photo",
        "gallery"
      )
    }
  }
}
