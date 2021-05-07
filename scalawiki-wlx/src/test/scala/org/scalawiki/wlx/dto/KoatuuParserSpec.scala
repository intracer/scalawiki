package org.scalawiki.wlx.dto

import org.specs2.mutable.Specification
import play.api.libs.json.{JsValue, Json}
import KoatuuNew.{makeHierarchy, parse}

class KoatuuParserSpec extends Specification {

  implicit def toJson(s: String): JsValue = Json.parse(s)

  val crimeaJson =
    """|{   "Перший рівень": "0100000000",
       |    "Другий рівень": "",
       |    "Третій рівень": "",
       |    "Четвертий рівень": "",
       |    "Категорія": "",
       |    "Назва об'єкта українською мовою": "АВТОНОМНА РЕСПУБЛІКА КРИМ/М.СІМФЕРОПОЛЬ"
       |}""".stripMargin

  val simferopolJson =
    """{  "Перший рівень": "0100000000",
      |   "Другий рівень": "0110100000",
      |   "Третій рівень": "",
      |   "Четвертий рівень": "",
      |   "Категорія": "",
      |   "Назва об'єкта українською мовою": "СІМФЕРОПОЛЬ"
      |}""".stripMargin

  val dniproRegionJson =
    """  {
      |    "Перший рівень": 1200000000,
      |    "Другий рівень": "",
      |    "Третій рівень": "",
      |    "Четвертий рівень": "",
      |    "Категорія": "",
      |    "Назва об'єкта українською мовою": "ДНІПРОПЕТРОВСЬКА ОБЛАСТЬ/М.ДНІПРО"
      |  }""".stripMargin

  val dniproCityJson =
    """  {
      |    "Перший рівень": 1200000000,
      |    "Другий рівень": 1210100000,
      |    "Третій рівень": "",
      |    "Четвертий рівень": "",
      |    "Категорія": "",
      |    "Назва об'єкта українською мовою": "ДНІПРО"
      |  }""".stripMargin

  def arr(s: String*): String = s.mkString("[", ",", "]")

  "parser" should {
    "parse Crimea" in {
      val regions = parse(arr(crimeaJson))
      regions.size === 1
      val crimea = regions.head
      crimea.code === "0100000000"
      crimea.name === "Автономна Республіка Крим"
    }

    "parse Simferopol" in {
      val regions = parse(arr(crimeaJson, simferopolJson))
      regions.size === 2
      val crimea = regions.head
      crimea.code === "0100000000"
      crimea.name === "Автономна Республіка Крим"

      val simferopol = regions.last
      simferopol.code === "0110100000"
      simferopol.name === "Сімферополь"
    }

    "parse Dnipro region" in {
      val regions = parse(arr(dniproRegionJson))
      regions.size === 1
      val dnipro = regions.head
      dnipro.code === "1200000000"
      dnipro.name === "Дніпропетровська область"
    }

    "parse Dnipro city" in {
      val regions = parse(arr(dniproCityJson))
      regions.size === 1
      val dnipro = regions.head
      dnipro.code === "1210100000"
      dnipro.name === "Дніпро"
    }
  }

  "make hierarchy" should {
    "parse Crimea" in {
      val regions = makeHierarchy(parse(arr(crimeaJson)))
      regions.size === 1
      val crimea = regions.head
      crimea.code === "0100000000"
      crimea.name === "Автономна Республіка Крим"
    }

    "parse Simferopol" in {
      val regions = makeHierarchy(parse(arr(crimeaJson, simferopolJson)))
      regions.size === 1
      val crimea = regions.head
      crimea.code === "0100000000"
      crimea.name === "Автономна Республіка Крим"

      crimea.regions.size === 1

      val simferopol = crimea.regions.head
      simferopol.code === "0110100000"
      simferopol.name === "Сімферополь"
    }
  }

}
