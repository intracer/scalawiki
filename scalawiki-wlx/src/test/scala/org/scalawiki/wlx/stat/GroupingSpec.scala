package org.scalawiki.wlx.stat

import org.scalawiki.dto.Image
import org.scalawiki.wlx.{Grouping, ImageGrouping}
import org.specs2.mutable.Specification

class GroupingSpec extends Specification {

  "grouping" should {
    "group by resolution" in {
      val images1 = Seq(Image("File:11.jpg", width = Some(1000), height = Some(1000)))
      val images2 = Seq(
        Image("File:21.jpg", width = Some(2000), height = Some(1000)),
        Image("File:22.jpg", width = Some(2000), height = Some(1100))
      )

      val g = new Grouping("mpx", ImageGrouping.byMpx, images1 ++ images2)

      g.keys === Set(1, 2)
      g.by(1) === images1
      g.by(2) === images2
      g.grouped === Map(1 -> images1, 2 -> images2)
    }

    "group by monument" in {
      val images1 = Seq(Image("File:11.jpg", monumentId = Some("123")))
      val images2 = Seq(
        Image("File:21.jpg", monumentId = Some("234")),
        Image("File:22.jpg", monumentId = Some("234"))
      )

      val g = new Grouping("monuments", ImageGrouping.byMonument, images1 ++ images2)

      g.keys === Set("123", "234")
      g.by("123") === images1
      g.by("234") === images2
      g.grouped === Map("123" -> images1, "234" -> images2)
    }

    "group by region" in {
      val images1 = Seq(Image("File:11.jpg", monumentId = Some("12-123")))
      val images2 = Seq(
        Image("File:21.jpg", monumentId = Some("13-234")),
        Image("File:22.jpg", monumentId = Some("13-234"))
      )

      val g = new Grouping("monuments", ImageGrouping.byRegion, images1 ++ images2)

      g.keys === Set("12", "13")
      g.by("12") === images1
      g.by("13") === images2
      g.grouped === Map("12" -> images1, "13" -> images2)
    }

    "group by author" in {
      val images1 = Seq(Image("File:11.jpg", author = Some("A1")))
      val images2 = Seq(
        Image("File:21.jpg", author = Some("B2")),
        Image("File:22.jpg", author = Some("B2"))
      )

      val g = new Grouping("monuments", ImageGrouping.byAuthor, images1 ++ images2)

      g.keys === Set("A1", "B2")
      g.by("A1") === images1
      g.by("B2") === images2
      g.grouped === Map("A1" -> images1, "B2" -> images2)
    }


  }
}
