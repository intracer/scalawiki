package org.scalawiki.wlx.stat.reports

import org.scalawiki.wlx.dto.{Country, Monument}
import org.specs2.mutable.Specification

class SpecialNominationsCountsSpec extends Specification {

  private val regionIds = Country.Ukraine.regionIds

  private def monument(id: String, page: String) = new Monument(id = id, name = id, page = page)

  "SpecialNominations.monumentCounts" should {
    "count a monument listed on two pages once" in {
      val monuments = Seq(
        monument("46-106-0001", "Львівська область дерев'яна архітектура"),
        monument("46-106-0001", "Пам'ятки дерев'яної архітектури України/Львівська область/1"),
        monument("46-106-0002", "Пам'ятки дерев'яної архітектури України/Львівська область/1")
      )
      SpecialNominations.monumentCounts(monuments, regionIds) === ((2, 0))
    }

    "count repeated special-nomination ids once" in {
      val monuments = Seq(
        monument("99-245-0001", "Єврейська спадщина/Львівська область"),
        monument("99-245-0001", "Єврейська спадщина/Чернівецька область"),
        monument("46-101-0001", "Єврейська спадщина/Львівська область")
      )
      SpecialNominations.monumentCounts(monuments, regionIds) === ((2, 1))
    }
  }
}
