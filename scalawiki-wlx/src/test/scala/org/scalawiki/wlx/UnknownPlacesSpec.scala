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

    emptyDb.unknownPlacesTables(Seq(place)) === Seq(
      Table(headers,
            Seq(
              Seq("place1", "", "regionId1-1 monument1")
            ),
            "page1"))
  }

  "Сичівка" in {
    val wiki =
      """{{ВЛП-рядок
      | ID = 71-246-0026
      | назва = Церква Іоанна Предтечі (мур.)
      | рік = 1896 р.
      | нас_пункт = с. [[Сичівка (Христинівський район)|Сичівка]]
    }}"""
    monumentDb(wiki).unknownPlaces() === Nil
  }

  "Микільське-на-Дніпрі" in {
    val wiki =
      """{{ВЛП-рядок
    | ID = 12-250-0138
    | назва = Кромлех
    | рік = кінець III тис. до н. е.
    | нас_пункт = [[Микільське-на-Дніпрі]]
    | адреса = на південь від села
  }}"""
    monumentDb(wiki).unknownPlaces() === Nil
  }

  "Bar" in {
    val wiki =
      """{{ВЛП-рядок
| ID = 05-202-0002
| назва = [[Будинок Коцюбинського (Бар)|Житловий будинок, в якому жив видатний український письменник М. М. Коцюбинський]]
| тип-нп = М
| нас_пункт = [[Бар (Україна)|Бар]]
  }}"""
    monumentDb(wiki).unknownPlaces() === Nil
  }

  "Вирішальне" in {
    val wiki =
      """{{ВЛП-рядок
| ID = 53-226-0051
| назва = Братська могила радянських воїнів
| нас_пункт =  с-щ. [[Вирішальне (Лохвицький район)|Вирішальне]]
  }}"""
    monumentDb(wiki).unknownPlaces() === Nil
  }

  "Рахни-Лісові" in {
    val wiki =
      """{{ВЛП-рядок
       || ID = 05-253-0039
       || назва = Пам'ятник 308 воїнам-односельчанам, загиблим на фронтах Великої Вітчизняної війни
       || рік = 1970
       || тип-нп = село
       || нас_пункт = [[Рахни-Лісові]]
       || адреса = біля Будинку культури
       || широта =
       || довгота =
       || охоронний номер = 480
       || тип = І-місц
       || фото = Рахни-Лісові P1760582.jpg
       || галерея = World War II memorial in Rakhny-Lisovi
       |}}
       |""".stripMargin
    monumentDb(wiki).unknownPlaces() === Nil
  }

  "same name in region not detected" in {
    val wiki =
      """{{ВЛП-рядок
        | ID = 14-227-0018
| назва = Братська могила радянських військовополонених
| рік =  1960 р.
| нас_пункт = с. Михайлівка
| адреса = вул. Шкільна,біля будівлі № 47
}}"""
    val db = monumentDb(wiki)
    db.unknownPlaces().size === 1
    db.unknownPlaces().head.candidates.size === 2
  }

  "same name in region detected" in {
    val wiki =
      """{{ВЛП-рядок
| ID = 14-227-0018
| назва = Братська могила радянських військовополонених
| рік =  1960 р.
| нас_пункт = [[Михайлівка (Покровський район, Михайлівська сільська рада)|Михайлівка (Михайлівська сільська рада)]]
| адреса = вул. Шкільна,біля будівлі № 47
}}"""
    monumentDb(wiki).unknownPlaces() === Nil
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
      Table(headers,
            Seq(
              Seq("place1", "", "regionId1-1 monument1"),
              Seq("place2", "", "regionId1-2 monument2")
            ),
            "page1"),
      Table(headers,
            Seq(
              Seq("place3", "", "regionId2-3 monument3"),
              Seq("place4", "", "regionId3-4 monument4")
            ),
            "page2")
    )
  }
}
