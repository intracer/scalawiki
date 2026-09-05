package org.scalawiki.wlx.dto

import org.specs2.mutable.Specification

import java.time.{Instant, LocalDate, ZoneId}

class ContestSpec extends Specification {

  "contest type" should {
    "contain wlm/e" in {
      ContestType.byCode("wlm") === Some(ContestType.WLM)
      ContestType.byCode("wle") === Some(ContestType.WLE)
      ContestType.byCode("wlx") === None
    }
  }

  "by campaign" should {
    "from config file" in {
      val c = Contest.byCampaign("wlm-ua").get
      c.country.code === "UA"
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
        "nameDetail",
        "year",
        "city",
        "cityType",
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

  "by codes" in {
    val c = Contest.byCampaign("wlm-bg").get
    c.country.code === "BG"
    c.country.name === "Bulgaria"
    c.contestType === ContestType.WLM
  }

  "contest dates" should {

    "read the WLM Ukraine upload window and pictured-date limit per year" in {
      val wlm = Contest.byCampaign("wlm-ua").get

      val d2019 = wlm.dates(2019).get
      d2019.uploadStart === Some(LocalDate.of(2019, 9, 1))
      d2019.uploadEnd === Some(LocalDate.of(2019, 9, 30))
      d2019.latestAllowedPicturedDate === None

      // Ukraine moved to October and added a security cut-off in 2022
      val d2022 = wlm.dates(2022).get
      d2022.uploadStart === Some(LocalDate.of(2022, 10, 1))
      d2022.uploadEnd === Some(LocalDate.of(2022, 10, 31))
      d2022.latestAllowedPicturedDate === Some(LocalDate.of(2022, 2, 23))

      wlm.dates(2026).get.latestAllowedPicturedDate === Some(LocalDate.of(2026, 8, 31))
    }

    "read WLE Ukraine dates, including the July shift and cross-year pictured limit" in {
      val wle = Contest.byCampaign("wle-ua").get

      wle.dates(2016).get.uploadEnd === Some(LocalDate.of(2016, 5, 31))
      wle.dates(2020).get.uploadEnd === Some(LocalDate.of(2020, 7, 31))
      wle.dates(2023).get.latestAllowedPicturedDate === Some(LocalDate.of(2022, 2, 23))
      wle.dates(2024).get.latestAllowedPicturedDate === Some(LocalDate.of(2024, 3, 31))
    }

    "interpret the dates as end-of-day in the configured Kyiv time zone" in {
      val wle = Contest.byCampaign("wle-ua").get.dates(2016).get
      wle.zone === ZoneId.of("Europe/Kyiv")
      // 2016-05-31 is firmly EEST (+03:00): end of day == 20:59:59 UTC
      wle.uploadEndInstant.map(_.toInstant) === Some(Instant.parse("2016-05-31T20:59:59Z"))

      val wlm = Contest.byCampaign("wlm-ua").get.dates(2025).get
      // 2025-08-31 EEST (+03:00)
      wlm.latestAllowedPicturedInstant.map(_.toInstant) ===
        Some(Instant.parse("2025-08-31T20:59:59Z"))
    }

    "return None for an unknown year or a config-less contest" in {
      Contest.byCampaign("wlm-ua").get.dates(1999) === None
      Contest(ContestType.WLM, Country.Ukraine, 2024).dates() === None
    }
  }
}
