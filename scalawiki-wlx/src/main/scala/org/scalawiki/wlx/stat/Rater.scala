package org.scalawiki.wlx.stat

import com.typesafe.config.Config
import org.scalawiki.MwBot
import org.scalawiki.dto.Image
import org.scalawiki.wlx.ImageDB

import scala.collection.mutable

case class RateConfig(newObjectRating: Option[Int] = None,
                      newAuthorObjectRating: Option[Int] = None,
                      numberOfAuthorsBonus: Boolean = false,
                      numberOfImagesBonus: Boolean = false)

object RateConfig {

  def apply(conf: StatParams): RateConfig = {
    apply(conf.newObjectRating.toOption,
      conf.newAuthorObjectRating.toOption,
      conf.numberOfAuthorsBonus.getOrElse(false),
      conf.numberOfImagesBonus.getOrElse(false)
    )
  }
}

trait Rater {

  def stat: ContestStat

  def imageDb: ImageDB = stat.currentYearImageDb.get

  def rate(monumentId: String, author: String): Int

  def explain(monumentId: String, author: String): String

  def rateMonumentIds(monumentIds: Set[String], author: String): Int = {
    monumentIds.toSeq.map(rate(_, author)).sum
  }

  def rateRegion(regionId: String, author: String): Int = {
    rateMonumentIds(imageDb._byAuthorAndRegion.by(author, regionId).flatMap(_.monumentId).toSet, author)
  }

  lazy val oldImages: Seq[Image] = {
    val totalImageDb = stat.totalImageDb.get
    val currentImageIds = imageDb.images.flatMap(_.pageId).toSet
    totalImageDb.images.filter(image => !currentImageIds.contains(image.pageId.get))
  }

  lazy val oldMonumentIds: Set[String] = oldImages.flatMap(_.monumentId).toSet

  def withRating: Boolean = true

}

object Rater {

  def create(stat: ContestStat): Rater = {
    stat.contest.config.map(fromConfig(stat, _)).getOrElse {
      val config = stat.contest.rateConfig

      val raters = Seq(new NumberOfMonuments(stat)) ++
        config.newAuthorObjectRating.map(r =>
          new NewlyPicturedPerAuthorBonus(stat, config.newObjectRating.getOrElse(1), r)
        ).orElse(
          config.newObjectRating.map(new NewlyPicturedBonus(stat, _))
        )

      if (raters.tail.isEmpty) {
        raters.head
      } else {
        new RateSum(stat, raters)
      }
    }
  }

  def fromConfig(stat: ContestStat, config: Config): Rater = {
    val rateCfg = config.getConfig(s"rates.${stat.contest.year}")
    val raters = Seq(new NumberOfMonuments(stat)) ++
      (if (rateCfg.hasPath("number-of-authors-bonus")) {
        Seq(new NumberOfAuthorsBonus(stat, RateRanges(rateCfg.getConfig("number-of-authors-bonus"))))
      } else Nil) ++
      (if (rateCfg.hasPath("number-of-images-bonus")) {
        Seq(new NumberOfImagesInPlaceBonus(stat, RateRanges(rateCfg.getConfig("number-of-images-bonus"))))
      } else Nil)

    if (raters.tail.isEmpty) {
      raters.head
    } else {
      new RateSum(stat, raters)
    }

  }
}

class NumberOfMonuments(val stat: ContestStat) extends Rater {
  val monumentIds = stat.monumentDb.map(_.ids).getOrElse(Set.empty)

  override def rate(monumentId: String, author: String): Int = {
    if (monumentIds.contains(monumentId)) 1 else 0
  }

  override def explain(monumentId: String, author: String): String = {
    if (monumentIds.contains(monumentId)) "Base rate = 1" else "Not a known monument = 0"
  }

  override def withRating: Boolean = false
}

class NewlyPicturedBonus(val stat: ContestStat, newlyPicturedRate: Int) extends Rater {

  override def rate(monumentId: String, author: String): Int = {
    if (!oldMonumentIds.contains(monumentId))
      newlyPicturedRate - 1
    else
      0
  }

  override def explain(monumentId: String, author: String): String = {
    if (!oldMonumentIds.contains(monumentId))
      s"Newly pictured rate bonus = ${newlyPicturedRate - 1}"
    else
      "Not newly pictured = 0"
  }
}

class NewlyPicturedPerAuthorBonus(val stat: ContestStat,
                                  newlyPicturedRate: Int,
                                  newlyPicturedPerAuthorRate: Int) extends Rater {

  val oldMonumentIdsByAuthor: Map[String, Set[String]] = oldImages
    .groupBy(_.author.getOrElse(""))
    .mapValues(_.flatMap(_.monumentId).toSet).toMap

  override def rate(monumentId: String, author: String): Int = {
    monumentId match {
      case id if !oldMonumentIds.contains(id) =>
        newlyPicturedRate - 1
      case id if !oldMonumentIdsByAuthor.getOrElse(author, Set.empty).contains(id) =>
        newlyPicturedPerAuthorRate - 1
      case _ =>
        0
    }
  }

  override def explain(monumentId: String, author: String): String = {
    monumentId match {
      case id if !oldMonumentIds.contains(id) =>
        s"Newly pictured bonus = ${newlyPicturedRate - 1}"
      case id if !oldMonumentIdsByAuthor.getOrElse(author, Set.empty).contains(id) =>
        s"Newly pictured per author bonus = ${newlyPicturedPerAuthorRate - 1}"
      case _ =>
        s"Not newly pictured = 0"
    }
  }
}

class NumberOfAuthorsBonus(val stat: ContestStat, val rateRanges: RateRanges) extends Rater {
  val authorsByMonument: Map[String, Set[String]] = oldImages.groupBy(_.monumentId.getOrElse(""))
    .mapValues { images =>
      images.map(_.author.getOrElse("")).toSet
    }.toMap

  val authorsNumberByMonument: Map[String, Int] = authorsByMonument.mapValues(_.size).toMap

  val distribution: Map[Int, Int] = authorsNumberByMonument.values.groupBy(identity).mapValues(_.size).toMap

  if (stat.config.exists(_.rateInputDistribution)) {
    new RateInputDistribution(stat, distribution, "Number of authors distribution",
      Seq("Number of authors", "Number of monuments")
    ).updateWiki(MwBot.fromHost(MwBot.commons))
  }

  override def rate(monumentId: String, author: String): Int = {
    if (rateRanges.sameAuthorZeroBonus && authorsByMonument.getOrElse(monumentId, Set.empty).contains(author)) {
      0
    } else {
      rateRanges.rate(authorsNumberByMonument.getOrElse(monumentId, 0))
    }
  }

  override def explain(monumentId: String, author: String): String = {
    val number = rate(monumentId, author)
    if (rateRanges.sameAuthorZeroBonus && authorsByMonument.getOrElse(monumentId, Set.empty).contains(author)) {
      s"Pictured by same author before = $number"
    } else {
      val picturedBy = authorsNumberByMonument.getOrElse(monumentId, 0)
      val (rate, start, end) = rateRanges.rateWithRange(picturedBy)
      s"Pictured before by $picturedBy ($start-${end.getOrElse("")}) authors = $rate"
    }
  }
}

case class PerPlaceStat(imagesPerPlace: Map[String, Int], placeByMonument: Map[String, String]) {
  val distribution = placeByMonument.values
    .map(place => imagesPerPlace.getOrElse(place, 0))
    .groupBy(identity).mapValues(_.size).toMap
}

object PerPlaceStat {

  val fallbackMapInverse = Map(
    "5121085201" -> Set("51-210-0002", "51-210-0011", "51-210-0080"), // Усатове
    "7125785201" -> Set("71-257-0008"), // Матусів
    "5322610199" -> Set("53-226-0068", "53-226-0069", "53-226-0070", "53-226-0071", "53-226-0072", "53-226-0073", "53-226-9001"), // урочище Шумейкове
    "2611040399" -> Set("26-110-0006"), // Говерла
    "0120481999" -> Set("01-204-0065"), // Голубинська сільська рада (Бахчисарайський район)
    "3222099719" -> Set("32-220-0071"), // центр колишнього села Красне
    "3222000739" -> Set("32-220-0073"), //колишнє село Купувате
    "3222000769" -> Set("32-220-0076"), //колишнє село Ладижичі
    "3222000799" -> Set("32-220-0079"), //колишнє село Машеве
    "3222000839" -> Set("32-220-0083", "32-220-0084"), //колишнє село Опачичі
    "3222000859" -> Set("32-220-0085"), //колишнє село Паришів
    "3222000889" -> Set("32-220-0088"), //колишнє село Розсоха
    "3222000899" -> Set("32-220-0089"), //колишнє село Роз'їждже
    "3222000919" -> Set("32-220-0091"), //колишнє село Старі Шепеличі
    "3222000929" -> Set("32-220-0092"), //колишнє село Старосілля
    "3222000939" -> Set("32-220-0093"), //колишнє село Стечанка
    "3222000949" -> Set("32-220-0094"), //колишнє село Стечанка
    "3222000969" -> Set("32-220-0096"), //центр колишнього села Товстий Ліс
    "3222000989" -> Set("32-220-0098"), //колишнє село Чапаєвка
    "3222001009" -> Set("32-220-0100"), //центр колишнього села Чистогалівка
    "0111590019" -> Set("01-115-0007", "01-115-0009", "01-115-0010", "01-115-9001"), //Армянська міськрада (Перекопський вал), за 5 км від міста
    "0122302009" -> Set("01-223-0041", "01-223-0103", "01-223-0104", "01-223-0200"), //Армянська та Красноперекопська міськради (на межі двох)
    "5322690019" -> Set("53-226-0068", "53-226-0069", "53-226-0070", "53-226-0071", "53-226-0072", "53-226-0073", "53-226-9001"), //урочище Шумейкове (15 км від лохвиці)
    "1423055701" -> Set("14-133-0017"), //uk:Торське (Лиманська міська громада)
    "1420989202" -> Set("14-209-0065"), //uk:Григорівка (Сіверська міська громада)
    "1422783201" -> Set("14-227-0018"), //uk:Михайлівка (Покровський район, Михайлівська сільська рада)
    "3220490019" -> Set("32-204-9001"), //між селами Городище-Пустоварівське Володарського і Щербаки Білоцерківського районів
    "5322610600" -> Set("53-226-0062"), //uk:Заводське
    "5324000829" -> Set("53-240-0082"), //ур. Ступки
    "4624801019" -> Set("46-248-0101") //Городиловичі (село більше не існує)  )
  )
  val fallbackMap = for ((koatuu, ids) <- fallbackMapInverse; id <- ids) yield id -> koatuu

  def apply(imageDB: ImageDB): PerPlaceStat = {
    val country = imageDB.contest.country
    val monumentDb = imageDB.monumentDb.get

    val imagesPerPlace = (for (id <- imageDB.ids.toSeq;
                               place <- monumentDb.placeByMonumentId.get(id))
      yield (place -> imageDB.byId(id).size)).groupBy(_._1).mapValues(_.map(_._2).sum)

    PerPlaceStat(imagesPerPlace.toMap, monumentDb.placeByMonumentId)
  }
}

class NumberOfImagesInPlaceBonus(val stat: ContestStat, val rateRanges: RateRanges) extends Rater {

  val oldImagesDb = new ImageDB(stat.contest, oldImages, stat.monumentDb)
  val perPlaceStat = PerPlaceStat(oldImagesDb)
  val unknownPlaceMonumentsByAuthor = mutable.Map[String, Set[String]]()
  val authorsByMonument: Map[String, Set[String]] = oldImages.groupBy(_.monumentId.getOrElse(""))
    .mapValues { images =>
      images.map(_.author.getOrElse("")).toSet
    }.toMap

  val distribution: Map[Int, Int] = perPlaceStat.distribution

  if (stat.config.exists(_.rateInputDistribution)) {
    new RateInputDistribution(stat, distribution, "Number of images in place distribution",
      Seq("Number of images in place", "Number of monuments")
    ).updateWiki(MwBot.fromHost(MwBot.commons))
  }

  override def rate(monumentId: String, author: String): Int = {
    if (rateRanges.sameAuthorZeroBonus && authorsByMonument.getOrElse(monumentId, Set.empty).contains(author)) {
      0
    } else {
      perPlaceStat.placeByMonument.get(monumentId).map { place =>
        rateRanges.rate(perPlaceStat.imagesPerPlace.getOrElse(place, 0))
      }.getOrElse {
        val monumentIds = unknownPlaceMonumentsByAuthor.getOrElse(author, Set.empty[String])
        if (!monumentIds.contains(monumentId)) {
          unknownPlaceMonumentsByAuthor(author) = monumentIds + monumentId
        }
        0
      }
    }
  }

  override def explain(monumentId: String, author: String): String = {
    if (rateRanges.sameAuthorZeroBonus && authorsByMonument.getOrElse(monumentId, Set.empty).contains(author)) {
      s"Pictured by same author before = 0"
    } else {
      perPlaceStat.placeByMonument.get(monumentId).map { place =>
        val perPlace = perPlaceStat.imagesPerPlace.getOrElse(place, 0)
        val (rate, start, end) = rateRanges.rateWithRange(perPlace)
        s"$perPlace ($start-${end.getOrElse("")}) images were of $place, bonus = $rate"
      }.getOrElse {
        val monumentIds = unknownPlaceMonumentsByAuthor.getOrElse(author, Set.empty[String])
        if (!monumentIds.contains(monumentId)) {
          unknownPlaceMonumentsByAuthor(author) = monumentIds + monumentId
        }
        s"unknown place of monument $monumentId, bonus = 0"
      }
    }
  }
}

class RateSum(val stat: ContestStat, val raters: Seq[Rater]) extends Rater {
  override def rate(monumentId: String, author: String): Int = {
    raters.map(_.rate(monumentId, author)).sum
  }

  override def explain(monumentId: String, author: String): String = {
    s"Rating = ${raters.map(_.rate(monumentId, author)).sum}, is a sum of: " + raters.map(_.explain(monumentId, author)).mkString(", ")
  }
}