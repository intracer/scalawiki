package org.scalawiki.wlx.stat

trait Rater {
  def rate(monumentId: String, author: String): Int
}

object NumberOfMonuments extends Rater {
  override def rate(monumentId: String, author: String) = 1
}

class NewlyPicturedBonus(oldMonumentIds: Set[String], newlyPicturedRate: Int) extends Rater {
  override def rate(monumentId: String, author: String): Int = {
    if (!oldMonumentIds.contains(monumentId))
      newlyPicturedRate
    else
      1
  }
}

class NewlyPicturedPerAuthorBonus(oldMonumentIds: Set[String],
                                  oldMonumentIdsByAuthor: Map[String, Set[String]],
                                  newlyPicturedRate: Int,
                                  newlyPicturedPerAuthorRate: Int) extends Rater {
  override def rate(monumentId: String, author: String): Int = {
    if (!oldMonumentIds.contains(monumentId))
      newlyPicturedRate
    else if (!oldMonumentIdsByAuthor.getOrElse(author, Set.empty).contains(monumentId))
      newlyPicturedPerAuthorRate
    else
      1
  }
}

class NumberOfAuthorsBonus(authorsByMonument: Map[String, Int]) extends Rater {
  override def rate(monumentId: String, author: String): Int = {
    val authors = authorsByMonument.getOrElse(monumentId, 0)

    if (0 == authors)
      5
    else if (1 <= authors && authors <= 3)
      2
    else if (4 <= authors && authors <= 9)
      1
    else
      0
  }
}

class NumberOfImagesInPlaceBonus(imagesPerPlace: Map[String, Int],
                                 placePerMonument: Map[String, String]) extends Rater {
  override def rate(monumentId: String, author: String): Int = {
    val bonus = placePerMonument.get(monumentId).map { place =>
      val images = imagesPerPlace.getOrElse(place, 0)

      if (0 == images)
        4
      else if (1 <= images && images <= 9)
        2
      else if (10 <= images && images <= 49)
        1
      else
        0
    }
    bonus.getOrElse(0)
  }
}

class RateSum(raters: Seq[Rater]) extends Rater {
  override def rate(monumentId: String, author: String): Int = {
    raters.map(_.rate(monumentId, author)).sum
  }
}