package org.scalawiki.wlx.stat

import com.concurrentthought.cla.Args
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

  def imageDb: ImageDB

  def rate(monumentId: String, author: String): Int

  def rateMonumentIds(monumentIds: Set[String], author: String): Int = monumentIds.map(rate(_, author)).sum

  def rateRegion(regionId: String, author: String): Int = {
    rateMonumentIds(imageDb._byAuthorAndRegion.by(author, regionId).flatMap(_.monumentId).toSet, author)
  }

}

object Rater {

  def create(imageDb: ImageDB, config: RateConfig): Rater = {
    new NumberOfMonuments(imageDb)
  }

}


class NumberOfMonuments(val imageDb: ImageDB) extends Rater {
  override def rate(monumentId: String, author: String) = 1
}

class NewlyPicturedBonus(val imageDb: ImageDB, oldMonumentIds: Set[String], newlyPicturedRate: Int) extends Rater {

  override def rate(monumentId: String, author: String): Int = {
    monumentId match {
      case id if !oldMonumentIds.contains(id) =>
        newlyPicturedRate
      case _ =>
        1
    }
  }
}

class NewlyPicturedPerAuthorBonus(val imageDb: ImageDB, oldMonumentIds: Set[String],
                                  oldMonumentIdsByAuthor: Map[String, Set[String]],
                                  newlyPicturedRate: Int,
                                  newlyPicturedPerAuthorRate: Int) extends Rater {
  override def rate(monumentId: String, author: String): Int = {
    monumentId match {
      case id if !oldMonumentIds.contains(id) =>
        newlyPicturedRate
      case id if !oldMonumentIdsByAuthor.getOrElse(author, Set.empty).contains(id) =>
        newlyPicturedPerAuthorRate
      case _ =>
        1
    }
  }
}

class NumberOfAuthorsBonus(val imageDb: ImageDB, authorsByMonument: Map[String, Int]) extends Rater {
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

class NumberOfImagesInPlaceBonus(val imageDb: ImageDB, imagesPerPlace: Map[String, Int],
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

class RateSum(val imageDb: ImageDB, raters: Seq[Rater]) extends Rater {
  override def rate(monumentId: String, author: String): Int = {
    raters.map(_.rate(monumentId, author)).sum
  }
}