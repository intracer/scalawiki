package org.scalawiki.wlx.stat

import org.scalawiki.dto.Image
import org.scalawiki.wlx.ImageDB

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

}

object Rater {

  def create(stat: ContestStat): Rater = {
    val config = stat.contest.rateConfig

    val raters = Seq(new NumberOfMonuments(stat)) ++
      config.newAuthorObjectRating.map(r =>
        new NewlyPicturedPerAuthorBonus(stat, config.newObjectRating.getOrElse(1), r)
      ).orElse(
        config.newObjectRating.map(new NewlyPicturedBonus(stat, _))
      ) ++ (if (config.numberOfAuthorsBonus) {
      Seq(new NumberOfAuthorsBonus(stat))
    } else Nil) ++ (if (config.numberOfImagesBonus) {
      Seq(new NumberOfImagesInPlaceBonus(stat))
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
}

class NewlyPicturedBonus(val stat: ContestStat, newlyPicturedRate: Int) extends Rater {

  override def rate(monumentId: String, author: String): Int = {
    if (!oldMonumentIds.contains(monumentId))
      newlyPicturedRate - 1
    else
      0
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
}

class NumberOfAuthorsBonus(val stat: ContestStat) extends Rater {
  val authorsByMonument: Map[String, Int] = oldImages
    .groupBy(_.monumentId.getOrElse(""))
    .mapValues { images =>
      images.map(_.author.getOrElse("")).toSet.size
    }.toMap

  override def rate(monumentId: String, author: String): Int = {
    authorsByMonument.getOrElse(monumentId, 0) match {
      case 0 =>
        6
      case x if (1 to 3) contains x =>
        2
      case x if (4 to 9) contains x =>
        1
      case _ =>
        0
    }
  }
}

case class PerPlaceStat(imagesPerPlace: Map[String, Int], placeByMonument: Map[String, String])

object PerPlaceStat {
  def apply(imageDB: ImageDB): PerPlaceStat = {
    val country = imageDB.contest.country
    val monumentDb = imageDB.monumentDb.get

    val placeByMonument = (for (id <- monumentDb.ids;
                                monument <- monumentDb.byId(id))
      yield {
        val regionId = id.split("-").take(2).mkString("-")
        val city = monument.city.getOrElse("")
        val candidates = country.byIdAndName(regionId, city)
        if (candidates.size == 1) {
          Some(id -> candidates.head.code)
        } else
          None
      }).flatten.toMap

    val imagesPerPlace = (for (id <- imageDB.ids.toSeq;
                               place <- placeByMonument.get(id))
      yield (place -> imageDB.byId(id).size)).groupBy(_._1).mapValues(_.map(_._2).sum)

    PerPlaceStat(imagesPerPlace.toMap, placeByMonument)
  }
}

class NumberOfImagesInPlaceBonus(val stat: ContestStat) extends Rater {

  val oldImagesDb = new ImageDB(stat.contest, oldImages, stat.monumentDb)
  val perPlaceStat = PerPlaceStat(oldImagesDb)

  override def rate(monumentId: String, author: String): Int = {
    perPlaceStat.placeByMonument.get(monumentId).map { place =>
      perPlaceStat.imagesPerPlace.getOrElse(place, 0) match {
        case 0 =>
          6
        case x if (1 to 9) contains x =>
          2
        case x if (10 to 49) contains x =>
          1
        case _ =>
          0
      }
    }.getOrElse(0)
  }
}

class RateSum(val stat: ContestStat, raters: Seq[Rater]) extends Rater {
  override def rate(monumentId: String, author: String): Int = {
    raters.map(_.rate(monumentId, author)).sum
  }
}