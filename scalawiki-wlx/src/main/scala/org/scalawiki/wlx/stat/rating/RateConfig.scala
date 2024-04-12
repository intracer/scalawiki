package org.scalawiki.wlx.stat.rating

case class RateConfig(
    newObjectRating: Option[Double] = None,
    newAuthorObjectRating: Option[Double] = None,
    numberOfAuthorsBonus: Boolean = false,
    numberOfImagesBonus: Boolean = false,
    baseRate: Double = 1
)
