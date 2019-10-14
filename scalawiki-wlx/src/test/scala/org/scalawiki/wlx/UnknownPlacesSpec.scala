package org.scalawiki.wlx

import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.specs2.mutable.Specification

class UnknownPlacesSpec extends Specification {

  val campaign = "wlm-ua"
  val contest = Contest.byCampaign(campaign).get.copy(year = 2019)
  val country = contest.country
  val listConfig = contest.uploadConfigs.head.listConfig

  val emptyDb = new MonumentDB(contest, Nil)
  val headers = Seq("name", "candidates", "monuments")

  private def monumentDb(wiki: String, page: String = "") = {
    val parser = new WlxTemplateParser(listConfig, page)
    val monuments = parser.parse(wiki).toSeq
    new MonumentDB(contest, monuments)
  }

  "report nothing" in {
    emptyDb.unknownPlacesTables(Nil) === Nil
  }

  "report one no candidates" in {
    val monument = new Monument("page1", "regionId1-1", "monument1")
    val place = UnknownPlace("page1", "regionId1", "place1", Nil, Seq(monument))

    emptyDb.unknownPlacesTables(Seq(place)) === Seq(Table(headers, Seq(
      Seq("place1", "", "monument1")
    ), "page1"))
  }

  "Сичівка" in {
    val wiki = """{{ВЛП-рядок
      | ID = 71-246-0026
      | назва = Церква Іоанна Предтечі (мур.)
      | рік = 1896 р.
      | нас_пункт = с. [[Сичівка (Христинівський район)|Сичівка]]
      | адреса =
      | широта =
      | довгота =
      | охоронний номер =
      | тип = А-щв.
      | фото =Сичівка, Христинівський р-н Черкащини. Дерев'яна Українська православна церква, 1850.TIF
      | галерея =
    }}"""
    val db = monumentDb(wiki)
    db.unknownPlaces() === Nil
  }

  "Микільське-на-Дніпрі" in {
    val wiki = """{{ВЛП-рядок
    | ID = 12-250-0138
    | назва = Кромлех
    | рік = кінець III тис. до н. е.
    | нас_пункт = [[Микільське-на-Дніпрі]]
    | адреса = на південь від села
    | широта =
    | довгота =
    | охоронний номер = 1064
    | тип = Х-місц
    | фото =
    | галерея =
  }}"""
    val db = monumentDb(wiki)
    db.unknownPlaces() === Nil
  }

  "report group by page" in {
    val monument1 = new Monument("page1", "regionId1-1", "monument1")
    val monument2 = new Monument("page1", "regionId1-2", "monument2")
    val monument3 = new Monument("page2", "regionId2-3", "monument3")
    val monument4 = new Monument("page2", "regionId3-4", "monument4")
    val places = Seq(
      UnknownPlace("page1", "regionId1", "place1", Nil, Seq(monument1)),
      UnknownPlace("page1", "regionId1", "place2", Nil, Seq(monument2)),
      UnknownPlace("page2", "regionId2", "place3", Nil, Seq(monument3)),
      UnknownPlace("page2", "regionId3", "place4", Nil, Seq(monument4))
    )

    emptyDb.unknownPlacesTables(places) === Seq(
      Table(headers, Seq(
        Seq("place1", "", "monument1"),
        Seq("place2", "", "monument2")
      ), "page1"),
      Table(headers, Seq(
        Seq("place3", "", "monument3"),
        Seq("place4", "", "monument4")
      ), "page2")
    )
  }
}