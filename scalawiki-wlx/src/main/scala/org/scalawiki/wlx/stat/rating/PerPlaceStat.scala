package org.scalawiki.wlx.stat.rating

import org.scalawiki.wlx.ImageDB

case class PerPlaceStat(
    imagesPerPlace: Map[String, Int],
    placeByMonument: Map[String, String]
) {
  val distribution = placeByMonument.values
    .map(place => imagesPerPlace.getOrElse(place, 0))
    .groupBy(identity)
    .mapValues(_.size)
    .toMap
}

object PerPlaceStat {

  val fallbackMapInverse = Map(
    "5121085201" -> Set("51-210-0002", "51-210-0011", "51-210-0080"), // Усатове
    "7125785201" -> Set("71-257-0008"), // Матусів
    "5322610199" -> Set(
      "53-226-0068",
      "53-226-0069",
      "53-226-0070",
      "53-226-0071",
      "53-226-0072",
      "53-226-0073",
      "53-226-9001"
    ), // урочище Шумейкове
    "2611040399" -> Set("26-110-0006"), // Говерла
    "0120481999" -> Set(
      "01-204-0065"
    ), // Голубинська сільська рада (Бахчисарайський район)
    "3222099719" -> Set("32-220-0071"), // центр колишнього села Красне
    "3222000739" -> Set("32-220-0073"), // колишнє село Купувате
    "3222000769" -> Set("32-220-0076"), // колишнє село Ладижичі
    "3222000799" -> Set("32-220-0079"), // колишнє село Машеве
    "3222000839" -> Set("32-220-0083", "32-220-0084"), // колишнє село Опачичі
    "3222000859" -> Set("32-220-0085"), // колишнє село Паришів
    "3222000889" -> Set("32-220-0088"), // колишнє село Розсоха
    "3222000899" -> Set("32-220-0089"), // колишнє село Роз'їждже
    "3222000919" -> Set("32-220-0091"), // колишнє село Старі Шепеличі
    "3222000929" -> Set("32-220-0092"), // колишнє село Старосілля
    "3222000939" -> Set("32-220-0093"), // колишнє село Стечанка
    "3222000949" -> Set("32-220-0094"), // колишнє село Стечанка
    "3222000969" -> Set("32-220-0096"), // центр колишнього села Товстий Ліс
    "3222000989" -> Set("32-220-0098"), // колишнє село Чапаєвка
    "3222001009" -> Set("32-220-0100"), // центр колишнього села Чистогалівка
    "0111590019" -> Set(
      "01-115-0007",
      "01-115-0009",
      "01-115-0010",
      "01-115-9001"
    ), // Армянська міськрада (Перекопський вал), за 5 км від міста
    "0122302009" -> Set(
      "01-223-0041",
      "01-223-0103",
      "01-223-0104",
      "01-223-0200"
    ), // Армянська та Красноперекопська міськради (на межі двох)
    "5322690019" -> Set(
      "53-226-0068",
      "53-226-0069",
      "53-226-0070",
      "53-226-0071",
      "53-226-0072",
      "53-226-0073",
      "53-226-9001"
    ), // урочище Шумейкове (15 км від лохвиці)
    "1423055701" -> Set("14-133-0017"), // uk:Торське (Лиманська міська громада)
    "1420989202" -> Set(
      "14-209-0065"
    ), // uk:Григорівка (Сіверська міська громада)
    "1422783201" -> Set(
      "14-227-0018"
    ), // uk:Михайлівка (Покровський район, Михайлівська сільська рада)
    "3220490019" -> Set(
      "32-204-9001"
    ), // між селами Городище-Пустоварівське Володарського і Щербаки Білоцерківського районів
    "5322610600" -> Set("53-226-0062"), // uk:Заводське
    "5324000829" -> Set("53-240-0082"), // ур. Ступки
    "4624801019" -> Set(
      "46-248-0101"
    ), // Городиловичі (село більше не існує)  )
    "5322681102" -> Set("53-226-0041"), // Забодаква — колишнє село
    "5322688402" -> Set("53-226-0110"), //
    "5322681910" -> Set("53-226-0117"), // Перевалівка
    "0111948301" -> Set("01-119-0349"),
    "0111949300" -> Set("01-119-0370"),
    "0111949702" -> Set("01-119-9002"),
    "5121085201" -> Set(
      "51-210-0074",
      "51-210-0075",
      "51-210-0082",
      "51-210-0002",
      "51-210-0011",
      "51-210-0080"
    ),
    "6524710101" -> Set("65-247-1251"),
    "4810800001" -> Set("48-108-0004"),
    "3222055103" -> Set("32-220-0060")
  )
  val fallbackMap =
    for ((koatuu, ids) <- fallbackMapInverse; id <- ids) yield id -> koatuu

  def apply(imageDB: ImageDB): PerPlaceStat = {
    val country = imageDB.contest.country
    val monumentDb = imageDB.monumentDb.get

    val imagesPerPlace = (for (
      id <- imageDB.ids.toSeq;
      place <- monumentDb.placeByMonumentId.get(id)
    )
      yield (place -> imageDB.byId(id).size))
      .groupBy(_._1)
      .mapValues(_.map(_._2).sum)

    PerPlaceStat(imagesPerPlace.toMap, monumentDb.placeByMonumentId)
  }
}