package org.scalawiki.wlx.dto

import org.scalawiki.wlx.dto.Country.Ukraine
import org.specs2.mutable.Specification

class KoatuuComparator extends Specification {

  val oldRegions = Ukraine.regions

  val UkraineNew: Country = new Country("UA", "Ukraine", Seq("uk"),
    Koatuu.regionsNew(() => Some(UkraineNew)))

  val newRegions = UkraineNew.regions

  "koatuus" should {
    "be equal" in {
      val kyiv1 = Ukraine.byIdAndName("80", "Київ").head
      val kyiv2 = UkraineNew.byIdAndName("80", "Київ").head

      kyiv1.code === kyiv2.code
      kyiv1.name === kyiv2.name

      kyiv1.regions.map(_.code) === kyiv2.regions.map(_.code)
      kyiv1.regions.map(_.name) === kyiv2.regions.map(_.name)
    }
  }

}
