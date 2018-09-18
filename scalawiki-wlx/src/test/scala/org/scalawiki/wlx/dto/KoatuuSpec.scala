package org.scalawiki.wlx.dto

import org.specs2.mutable.Specification

class KoatuuSpec extends Specification {

  "Koatuu" should {

    val regions = Koatuu.regions

    "contain level1" in {
      regions.map(_.copy(regions = Nil)) === Country.Ukraine.regions
    }

    "contain Kyiv raions" in {
      val kyiv = regions.find(_.name == "Київ").get
      kyiv.regions.map(_.name) === Seq("Райони м. Київ", "Голосіївський", "Дарницький", "Деснянський", "Дніпровський",
        "Оболонський", "Печерський", "Подільський", "Святошинський", "Солом'янський", "Шевченківський")
    }

    "contain Crimea regions" in {
      val crimea = regions.find(_.name == "Автономна Республіка Крим").get
      crimea.regions.map(_.name) === Seq(
        "Міста Автономної Республіки Крим",
        "Сімферополь", "Алушта", "Джанкой", "Євпаторія", "Керч",
        "Красноперекопськ", "Саки", "Армянськ", "Феодосія", "Судак", "Ялта",
        "Райони Автономної Республіки Крим",
        "Бахчисарайський район", "Білогірський район", "Джанкойський район", "Кіровський район", "Красногвардійський район",
        "Красноперекопський район", "Ленінський район", "Нижньогірський район", "Первомайський район", "Роздольненський район",
        "Сакський район", "Сімферопольський район", "Совєтський район", "Чорноморський район")
    }

    "contain Vinnytsya oblast regions" in {
      val crimea = regions.find(_.name == "Вінницька область").get
      crimea.regions.map(_.name) === Seq(
        "Міста обласного підпорядкування Вінницької області",
        "Вінниця", "Жмеринка", "Могилів-Подільський", "Козятин", "Ладижин", "Хмільник",
        "Райони Вінницької області",
        "Барський район", "Бершадський район", "Вінницький район", "Гайсинський район", "Жмеринський район",
        "Іллінецький район", "Козятинський район", "Калинівський район", "Крижопільський район", "Липовецький район",
        "Літинський район", "Могилів-Подільський район", "Мурованокуриловецький район", "Немирівський район",
        "Оратівський район", "Піщанський район", "Погребищенський район", "Теплицький район", "Томашпільський район",
        "Тростянецький район", "Тульчинський район", "Тиврівський район", "Хмільницький район", "Чернівецький район",
        "Чечельницький район", "Шаргородський район", "Ямпільський район")
    }

  }
}
