package org.scalawiki.wlx.dto

import org.specs2.mutable.Specification

class KoatuuSpec extends Specification {

  val Ukraine = Country.Ukraine
  val regions = Ukraine.regions

  "level1" should {

    val topRegions = Map(
      "01" -> "Автономна Республіка Крим",
      "05" -> "Вінницька область",
      "07" -> "Волинська область",
      "12" -> "Дніпропетровська область",
      "14" -> "Донецька область",
      "18" -> "Житомирська область",
      "21" -> "Закарпатська область",
      "23" -> "Запорізька область",
      "26" -> "Івано-Франківська область",
      "32" -> "Київська область",
      "35" -> "Кіровоградська область",
      "44" -> "Луганська область",
      "46" -> "Львівська область",
      "48" -> "Миколаївська область",
      "51" -> "Одеська область",
      "53" -> "Полтавська область",
      "56" -> "Рівненська область",
      "59" -> "Сумська область",
      "61" -> "Тернопільська область",
      "65" -> "Херсонська область",
      "63" -> "Харківська область",
      "68" -> "Хмельницька область",
      "71" -> "Черкаська область",
      "73" -> "Чернівецька область",
      "74" -> "Чернігівська область",
      "80" -> "Київ",
      "85" -> "Севастополь"
    )

    "contain country parent" in {
      regions.flatMap(_.parent().map(_.name)) === List.fill(topRegions.size)(Country.Ukraine.name)
    }

    "have 27 elements" in {
      regions.size === 27
    }

    "contain level1 names" in {
      regions.map(_.name).toSet === topRegions.toSeq.map(_._2).toSet
    }

    "lookup level1 by code" in {
      Ukraine.byRegion(topRegions.keySet)
        .map { case (adm, ids) => ids.head -> adm.name } === topRegions

      Ukraine.byIdAndName("80-361", "Київ").head.name === "Київ"
    }

    "lookup by different codes should be same" in {
      val kyiv1 = Ukraine.byIdAndName("80", "Київ").head
      val kyiv2 = Ukraine.byIdAndName("80-361", "Київ").head
      val kyiv3 = Ukraine.byIdAndName("80-361-0", "Київ").head

      kyiv1 === kyiv2
      kyiv2 === kyiv3
    }
  }

  "level2" should {

    "have 490 raions" in {
      val withoutCities = Country.Ukraine.regions.filter(adm => !Set("Київ", "Севастополь").contains(adm.name))
      withoutCities.size === 25

      val raions = withoutCities.flatMap(_.regions).filter(_.name.endsWith("район"))
      raions.size === 490
    }

    "contain Kyiv raions" in {
      val regionNames = Seq("Голосіївський", "Дарницький", "Деснянський", "Дніпровський",
        "Оболонський", "Печерський", "Подільський", "Святошинський", "Солом'янський", "Шевченківський")
      val kyiv = regions.find(_.name == "Київ").get
      kyiv.regions.map(_.name) === regionNames
      kyiv.regions.flatMap(_.parent().map(_.name)) === List.fill(regionNames.size)("Київ")
    }

    "find Kyiv raions by code" in {
      val idToName = Map(
        "80-361" -> "Голосіївський",
        "80-363" -> "Дарницький",
        "80-364" -> "Деснянський",
        "80-366" -> "Дніпровський",
        "80-380" -> "Оболонський",
        "80-382" -> "Печерський",
        "80-385" -> "Подільський",
        "80-386" -> "Святошинський",
        "80-389" -> "Солом'янський",
        "80-391" -> "Шевченківський"
      )

      val regionToIds = Ukraine.byRegion(idToName.keySet).mapValues(_.head)
      regionToIds.keySet.flatMap(_.parent().map(_.name)) === Set("Київ")

      regionToIds.map(_.swap).mapValues(_.name) === idToName
    }

    "contain Kyiv oblast regions" in {
      val ko = regions.find(_.name == "Київська область").get
      val koRegions = Seq(
        "Баришівський", "Білоцерківський", "Богуславський", "Бориспільський", "Бородянський", "Броварський",
        "Васильківський", "Вишгородський", "Володарський", "Згурівський", "Іванківський", "Кагарлицький", "Києво-Святошинський",
        "Макарівський", "Миронівський", "Обухівський", "Переяслав-Хмельницький", "Поліський", "Рокитнянський", "Сквирський",
        "Ставищенський", "Таращанський", "Тетіївський", "Фастівський", "Яготинський"
      ).map(_ + " район")

      val koCities = Seq("Біла Церква", "Березань", "Бориспіль", "Бровари", "Буча", "Васильків", "Ірпінь", "Обухів", "Переяслав-Хмельницький",
        "Прип'ять", "Ржищів", "Славутич", "Фастів")

      val names = Seq() ++ koCities ++ koRegions
      ko.regions.map(_.name).toSet === names.toSet

      ko.regions.flatMap(_.parent().map(_.name)) === List.fill(names.size)("Київська область")
    }

    "contain Crimea regions" in {
      val crimea = regions.find(_.name == "Автономна Республіка Крим").get
      val regionNames = Seq(
        "Сімферополь", "Алушта", "Джанкой", "Євпаторія", "Керч",
        "Красноперекопськ", "Саки", "Армянськ", "Феодосія", "Судак", "Ялта",
        "Бахчисарайський район", "Білогірський район", "Джанкойський район", "Кіровський район", "Красногвардійський район",
        "Красноперекопський район", "Ленінський район", "Нижньогірський район", "Первомайський район", "Роздольненський район",
        "Сакський район", "Сімферопольський район", "Совєтський район", "Чорноморський район")
      crimea.regions.map(_.name) === regionNames

      crimea.regions.flatMap(_.parent().map(_.name)) === List.fill(regionNames.size)("Автономна Республіка Крим")
    }

    "contain Vinnytsya oblast regions" in {
      val crimea = regions.find(_.name == "Вінницька область").get
      val regionNames = Seq(
        "Вінниця", "Жмеринка", "Могилів-Подільський", "Козятин", "Ладижин", "Хмільник",
        "Барський район", "Бершадський район", "Вінницький район", "Гайсинський район", "Жмеринський район",
        "Іллінецький район", "Козятинський район", "Калинівський район", "Крижопільський район", "Липовецький район",
        "Літинський район", "Могилів-Подільський район", "Мурованокуриловецький район", "Немирівський район",
        "Оратівський район", "Піщанський район", "Погребищенський район", "Теплицький район", "Томашпільський район",
        "Тростянецький район", "Тульчинський район", "Тиврівський район", "Хмільницький район", "Чернівецький район",
        "Чечельницький район", "Шаргородський район", "Ямпільський район")
      crimea.regions.map(_.name) === regionNames

      crimea.regions.flatMap(_.parent().map(_.name)) === List.fill(regionNames.size)("Вінницька область")
    }

    "lookup regions by monumentId" in {
      val r1 = Ukraine.byId("14-215-0078").get
      r1.name === "Волноваський район"
      r1.parent().get.name === "Донецька область"

      val r2 = Ukraine.byId("26-252-0002").get
      r2.name === "Снятинський район"
      r2.parent().get.name === "Івано-Франківська область"
    }

    "differentiate by name and id" in {
      val regs = Ukraine.byIdAndName("23-101-1234", "[[Запоріжжя]]")
      regs.size === 1

      val r = regs.head
      r.name === "Запоріжжя"
      r.code === "23101"

      val parent = r.parent().get
      parent.name === "Запорізька область"
      parent.code === "23"
    }
  }

  "level3/4" should {
    "contain Irpin regions" in {
      val irpin = Ukraine.byId("32-109").get
      irpin.name === "Ірпінь"
      irpin.regions.map(_.name) === Seq("Ворзель", "Гостомель", "Коцюбинське")

      Ukraine.byIdAndName("32-109", "Ворзель").head.name === "Ворзель"
    }

    "contain Obuhiv regions" in {
      val obuhiv = Ukraine.byId("32-116").get
      obuhiv.name === "Обухів"
      obuhiv.regions.map(_.name) === Seq("Ленди", "Таценки")

      Ukraine.byIdAndName("32-116", "Ленди").head.name === "Ленди"
    }

    "contain Simferopol regions" in {
      val simferopol = Ukraine.byId("01-101").get
      simferopol.name === "Сімферополь"
      simferopol.regions.map(_.name) === Seq("Залізничний", "Київський", "Центральний", "Аерофлотський", "Гресівський",
        "Комсомольське", "Аграрне")

      val hresivskyi = simferopol.regions.find(_.name == "Гресівський").get
      hresivskyi.regions.map(_.name) === Seq("Бітумне")

      Ukraine.byIdAndName("01-101", "Бітумне").head.name === "Бітумне"
    }

    "contain only one" in {
      Ukraine.byIdAndName("53-236", "Тарасівка").size === 1
      Ukraine.byIdAndName("14-242", "Миколаївка").size === 1
    }

    "differentiate types" in {
      val smt = Ukraine.byIdAndName("14-215", "смт Андріївка")
      val selo1 = Ukraine.byIdAndName("14-215", "село Андріївка")
      val selo2 = Ukraine.byIdAndName("14-215", "[[Андріївка (Волноваський район, село)|Андріївка (село)]]")
      smt.size === 1
      smt.head.regionType === Some(RegionTypes.codeToType("Т"))
      selo1.size === 1
      selo1.head.regionType === Some(RegionTypes.codeToType("С"))
      selo2.size === 1
      selo2.head.regionType === Some(RegionTypes.codeToType("С"))
    }

    "contain lesser regions" in {
      Ukraine.byIdAndName("18-240", "Новоград-Волинський район").size === 1
      Ukraine.byIdAndName("14-224", "Іванопільська").size === 1  // сільська рада
      Ukraine.byIdAndName("18-211", "Хорошівський район").size === 1
      Ukraine.byIdAndName("35-236", "Новоархангельський район").size === 1
      Ukraine.byIdAndName("18-254", "Пулинський район").size === 1
//      Ukraine.byIdAndName("01-116", "Феодосійська").size === 1  //  м/р
//      Ukraine.byIdAndName("01-119", "Ялтинська").size === 1  //  м/р
    }
  }
}