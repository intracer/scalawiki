package org.scalawiki.wlx.stat

import com.concurrentthought.cla.Args
import org.scalawiki.dto.Image
import org.scalawiki.wlx.ImageDB

case class RateConfig(newObjectRating: Option[Int] = None,
                      newAuthorObjectRating: Option[Int] = None,
                      numberOfAuthorsBonus: Boolean = false,
                      numberOfImagesBonus: Boolean = false)

object RateConfig {

  def apply(args: Args): RateConfig = {
    val newObjectRating = args.values.get("new-object-rating").asInstanceOf[Option[Int]]
    val newAuthorObjectRating = args.values.get("new-author-object-rating").asInstanceOf[Option[Int]]
    val numberOfAuthorsBonus = args.values.get("number-of-authors-bonus").asInstanceOf[Option[Boolean]].getOrElse(false)
    val numberOfImagesBonus = args.values.get("number-of-images-bonus").asInstanceOf[Option[Boolean]].getOrElse(false)

    apply(newObjectRating, newAuthorObjectRating, numberOfAuthorsBonus, numberOfImagesBonus)
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
      config.newAuthorObjectRating.map { r =>
        new NewlyPicturedPerAuthorBonus(stat, config.newObjectRating.getOrElse(1), r)
      }.orElse {
        config.newObjectRating.map(new NewlyPicturedBonus(stat, _))
      } ++ (if (config.numberOfAuthorsBonus) {
      Seq(new NumberOfAuthorsBonus(stat))
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
    .mapValues(_.flatMap(_.monumentId).toSet)

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
    }

  override def rate(monumentId: String, author: String): Int = {
    authorsByMonument.getOrElse(monumentId, 0) match {
      case 0 =>
        5
      case x if (1 to 3) contains x =>
        2
      case x if (4 to 9) contains x =>
        1
      case _ =>
        0
    }
  }
}

class NumberOfImagesInPlaceBonus(val stat: ContestStat, imagesPerPlace: Map[String, Int],
                                 placeByMonument: Map[String, String]) extends Rater {
  override def rate(monumentId: String, author: String): Int = {
    placeByMonument.get(monumentId).map { place =>
      imagesPerPlace.getOrElse(place, 0) match {
        case 0 =>
          4
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