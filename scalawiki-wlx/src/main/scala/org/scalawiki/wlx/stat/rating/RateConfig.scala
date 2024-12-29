package org.scalawiki.wlx.stat.rating

import org.scalawiki.wlx.stat.StatParams

case class RateConfig(
    newObjectRating: Option[Double] = None,
    newAuthorObjectRating: Option[Double] = None,
    numberOfAuthorsBonus: Boolean = false,
    numberOfImagesBonus: Boolean = false,
    baseRate: Double = 1
)

object RateConfig {

  def apply(conf: StatParams): RateConfig = {
    apply(
      conf.newObjectRating.toOption,
      conf.newAuthorObjectRating.toOption,
      conf.numberOfAuthorsBonus.getOrElse(false),
      conf.numberOfImagesBonus.getOrElse(false),
      conf.baseRate.getOrElse(1)
    )
  }
}
