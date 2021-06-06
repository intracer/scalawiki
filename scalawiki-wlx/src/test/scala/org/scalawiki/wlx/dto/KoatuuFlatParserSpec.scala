package org.scalawiki.wlx.dto

import org.specs2.mutable.Specification
import play.api.libs.json.{JsValue, Json}
import KoatuuNew.parse
import AdmDivisionFlat.makeHierarchy

class KoatuuFlatParserSpec extends Specification {

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

  val simferopolRegionJson =
    """  {
      |    "Перший рівень": "0100000000",
      |    "Другий рівень": "0110100000",
      |    "Третій рівень": "0110136300",
      |    "Четвертий рівень": "",
      |    "Категорія": "Р",
      |    "Назва об'єкта українською мовою": "ЗАЛІЗНИЧНИЙ"
      |  }
      |""".stripMargin

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

  val kyivJson =
    """[{
      |    "Перший рівень": 8000000000,
      |    "Другий рівень": "",
      |    "Третій рівень": "",
      |    "Четвертий рівень": "",
      |    "Категорія": "",
      |    "Назва об'єкта українською мовою": "М.КИЇВ"
      |  },
      |  {
      |    "Перший рівень": 8000000000,
      |    "Другий рівень": 8030000000,
      |    "Третій рівень": "",
      |    "Четвертий рівень": "",
      |    "Категорія": "",
      |    "Назва об'єкта українською мовою": "РАЙОНИ М. КИЇВ"
      |  },
      |  {
      |    "Перший рівень": 8000000000,
      |    "Другий рівень": 8036100000,
      |    "Третій рівень": "",
      |    "Четвертий рівень": "",
      |    "Категорія": "Р",
      |    "Назва об'єкта українською мовою": "ГОЛОСІЇВСЬКИЙ"
      |  },
      |  {
      |    "Перший рівень": 8000000000,
      |    "Другий рівень": 8036300000,
      |    "Третій рівень": "",
      |    "Четвертий рівень": "",
      |    "Категорія": "Р",
      |    "Назва об'єкта українською мовою": "ДАРНИЦЬКИЙ"
      |  },
      |  {
      |    "Перший рівень": 8000000000,
      |    "Другий рівень": 8036400000,
      |    "Третій рівень": "",
      |    "Четвертий рівень": "",
      |    "Категорія": "Р",
      |    "Назва об'єкта українською мовою": "ДЕСНЯНСЬКИЙ"
      |  },
      |  {
      |    "Перший рівень": 8000000000,
      |    "Другий рівень": 8036600000,
      |    "Третій рівень": "",
      |    "Четвертий рівень": "",
      |    "Категорія": "Р",
      |    "Назва об'єкта українською мовою": "ДНІПРОВСЬКИЙ"
      |  },
      |  {
      |    "Перший рівень": 8000000000,
      |    "Другий рівень": 8038000000,
      |    "Третій рівень": "",
      |    "Четвертий рівень": "",
      |    "Категорія": "Р",
      |    "Назва об'єкта українською мовою": "ОБОЛОНСЬКИЙ"
      |  },
      |  {
      |    "Перший рівень": 8000000000,
      |    "Другий рівень": 8038200000,
      |    "Третій рівень": "",
      |    "Четвертий рівень": "",
      |    "Категорія": "Р",
      |    "Назва об'єкта українською мовою": "ПЕЧЕРСЬКИЙ"
      |  },
      |  {
      |    "Перший рівень": 8000000000,
      |    "Другий рівень": 8038500000,
      |    "Третій рівень": "",
      |    "Четвертий рівень": "",
      |    "Категорія": "Р",
      |    "Назва об'єкта українською мовою": "ПОДІЛЬСЬКИЙ"
      |  },
      |  {
      |    "Перший рівень": 8000000000,
      |    "Другий рівень": 8038600000,
      |    "Третій рівень": "",
      |    "Четвертий рівень": "",
      |    "Категорія": "Р",
      |    "Назва об'єкта українською мовою": "СВЯТОШИНСЬКИЙ"
      |  },
      |  {
      |    "Перший рівень": 8000000000,
      |    "Другий рівень": 8038900000,
      |    "Третій рівень": "",
      |    "Четвертий рівень": "",
      |    "Категорія": "Р",
      |    "Назва об'єкта українською мовою": "СОЛОМ'ЯНСЬКИЙ"
      |  },
      |  {
      |    "Перший рівень": 8000000000,
      |    "Другий рівень": 8039100000,
      |    "Третій рівень": "",
      |    "Четвертий рівень": "",
      |    "Категорія": "Р",
      |    "Назва об'єкта українською мовою": "ШЕВЧЕНКІВСЬКИЙ"
      |}]""".stripMargin

  def arr(s: String*): String = s.mkString("[", ",", "]")

  "parser" should {
    "parse Crimea" in {
      val regions = parse(arr(crimeaJson))
      regions.size === 1
      val crimea = regions.head
      crimea.code === "01"
      crimea.name === "Автономна Республіка Крим"
    }

    "parse Simferopol" in {
      val regions = parse(arr(crimeaJson, simferopolJson, simferopolRegionJson))
      regions.size === 3
      val crimea = regions.head
      crimea.code === "01"
      crimea.name === "Автономна Республіка Крим"

      val simferopol = regions.init.last
      simferopol.code === "01101"
      simferopol.name === "Сімферополь"

      val simferopolRegion = regions.last
      simferopolRegion.code === "0110136300"
      simferopolRegion.name === "Залізничний"

    }

    "parse Dnipro region" in {
      val regions = parse(arr(dniproRegionJson))
      regions.size === 1
      val dnipro = regions.head
      dnipro.code === "12"
      dnipro.name === "Дніпропетровська область"
    }

    "parse Dnipro city" in {
      val regions = parse(arr(dniproCityJson))
      regions.size === 1
      val dnipro = regions.head
      dnipro.code === "12101"
      dnipro.name === "Дніпро"
    }
    "parse Kyiv city" in {
      val regions = parse(kyivJson)
      regions.size === 12
      val kyiv = regions.head
      kyiv.code === "80"
      kyiv.name === "Київ"
      regions.tail.map(_.name) === Seq("Райони м. Київ", "Голосіївський", "Дарницький", "Деснянський", "Дніпровський",
        "Оболонський", "Печерський", "Подільський", "Святошинський", "Солом'янський", "Шевченківський")
    }
  }

  "make hierarchy" should {
    "parse Crimea" in {
      val regions = makeHierarchy(parse(arr(crimeaJson)))
      regions.size === 1
      val crimea = regions.head
      crimea.code === "01"
      crimea.name === "Автономна Республіка Крим"
      crimea.byMonumentId("01") === Some(crimea)
    }

    "parse Simferopol" in {
      val regions = makeHierarchy(parse(arr(crimeaJson, simferopolJson, simferopolRegionJson)))
      regions.size === 1
      val crimea = regions.head
      crimea.code === "01"
      crimea.name === "Автономна Республіка Крим"

      crimea.regions.size === 1

      val simferopol = crimea.regions.head
      simferopol.code === "01101"
      simferopol.name === "Сімферополь"

      crimea.regions.flatMap(_.parent().map(_.name)) === Seq("Автономна Республіка Крим")
    }

    "parse Kyiv city" in {
      val regions = makeHierarchy(parse(kyivJson))
      regions.size === 1
      val kyiv = regions.head
      kyiv.code === "80"
      kyiv.name === "Київ"
      kyiv.regions.map(_.name) === Seq("Голосіївський", "Дарницький", "Деснянський", "Дніпровський",
        "Оболонський", "Печерський", "Подільський", "Святошинський", "Солом'янський", "Шевченківський")
    }
  }

}
