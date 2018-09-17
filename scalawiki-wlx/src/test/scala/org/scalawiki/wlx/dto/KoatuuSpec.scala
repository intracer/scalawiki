package org.scalawiki.wlx.dto

import org.specs2.mutable.Specification

class KoatuuSpec extends Specification {

  "Koatuu" should {

    val regions = Koatuu.regions

    "contain level1" in {
      regions.map(_.copy(regions = Nil)) === Country.Ukraine.regions
    }

    "contain Kyiv raions" in {
      val kyiv = regions.find(_.name == "м. Київ").get
      kyiv.regions.map(_.name) === Seq("Райони м. Київ", "Голосіївський", "Дарницький", "Деснянський", "Дніпровський",
        "Оболонський", "Печерський", "Подільський", "Святошинський", "Солом'янський", "Шевченківський")
    }
  }
}
