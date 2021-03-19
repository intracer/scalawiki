package org.scalawiki.wlx

import org.scalawiki.wlx.dto.lists.ListConfig
import org.specs2.mutable.Specification

import scala.collection.immutable.ListMap

class ListConfigSpec extends Specification {

  "list config" should {
    "parse wlm ua" in {
      val lc = ListConfig.WlmUa

      lc.namesMap === ListMap(
        "ID" -> "ID",
        "name" -> "назва",
        "year" -> "рік",
        "city" -> "нас_пункт",
        "cityType" -> "тип-нп",
        "place" -> "адреса",
        "lat" -> "широта",
        "lon" -> "довгота",
        "stateId" -> "охоронний номер",
        "type" -> "тип",
        "photo" -> "фото",
        "gallery" -> "галерея"
      )

      lc.templateName === "ВЛП-рядок"
    }

    "parse wle ua" in {
      val lc = ListConfig.WleUa

      lc.namesMap === ListMap(
        "ID" -> "ID",
        "name" -> "назва",
        "resolution" -> "постанова",
        "place" -> "розташування",
        "user" -> "користувач",
        "area" -> "площа",
        "lat" -> "широта",
        "lon" -> "довгота",
        "type" -> "тип",
        "subType" -> "підтип",
        "photo" -> "фото",
        "gallery" -> "галерея"
      )

      lc.templateName === "ВЛЗ-рядок"
    }

    "parse wle th" in {
      val lc = ListConfig.WleTh

      lc.namesMap === ListMap(
        "ID" -> "ลำดับ",
        "name" -> "อุทยานแห่งชาติ",
        "area" -> "พื้นที่",
        "year" -> "ปีที่จัดตั้ง",
        "photo" -> "ไฟล์",
        "gallery" -> "commonscat"
      )

      lc.templateName === "อุทยานแห่งชาติในประเทศไทย WLE"
    }
  }

}
