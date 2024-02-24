package org.scalawiki.wlx.stat

import org.scalawiki.dto.Image
import org.scalawiki.wlx.{Grouping, ImageGrouping}
import org.specs2.mutable.Specification

class GroupingSpec extends Specification {

  "grouping" should {
    "group by resolution" in {
      val images1 =
        Seq(Image("File:11.jpg", width = Some(1000), height = Some(1000)))
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
      val images1 = Seq(Image("File:11.jpg", monumentIds = List("123")))
      val images2 = Seq(
        Image("File:21.jpg", monumentIds = List("234")),
        Image("File:22.jpg", monumentIds = List("234"))
      )

      val g =
        new Grouping("monuments", ImageGrouping.byMonument, images1 ++ images2)

      g.keys === Set("123", "234")
      g.by("123") === images1
      g.by("234") === images2
      g.grouped === Map("123" -> images1, "234" -> images2)
    }

    "group by region" in {
      val images1 = Seq(Image("File:11.jpg", monumentIds = List("01-123")))
      val images2 = Seq(
        Image("File:21.jpg", monumentIds = List("05-234")),
        Image("File:22.jpg", monumentIds = List("05-234"))
      )

      val g =
        new Grouping("monuments", ImageGrouping.byRegion, images1 ++ images2)

      g.keys === Set("01", "05")
      g.by("01") === images1
      g.by("05") === images2
      g.grouped === Map("01" -> images1, "05" -> images2)
    }

    "group by author" in {
      val images1 = Seq(Image("File:11.jpg", author = Some("A1")))
      val images2 = Seq(
        Image("File:21.jpg", author = Some("B2")),
        Image("File:22.jpg", author = Some("B2"))
      )

      val g =
        new Grouping("monuments", ImageGrouping.byAuthor, images1 ++ images2)

      g.keys === Set("A1", "B2")
      g.by("A1") === images1
      g.by("B2") === images2
      g.grouped === Map("A1" -> images1, "B2" -> images2)
    }

    "group by author and region" in {
      val imagesA1 = Seq(
        Image("File:A1.jpg", author = Some("A"), monumentIds = List("01-123"))
      )
      val imagesA2 = Seq(
        Image("File:A21.jpg", author = Some("A"), monumentIds = List("05-345")),
        Image("File:A22.jpg", author = Some("A"), monumentIds = List("05-345"))
      )
      val imagesB1 = Seq(
        Image("File:B1.jpg", author = Some("B"), monumentIds = List("01-123"))
      )
      val imagesB2 = Seq(
        Image("File:B21.jpg", author = Some("B"), monumentIds = List("05-345")),
        Image("File:B22.jpg", author = Some("B"), monumentIds = List("05-345"))
      )
      val all = imagesA1 ++ imagesA2 ++ imagesB1 ++ imagesB2

      val byA = new Grouping("monuments", ImageGrouping.byAuthor, all)
      val byAR = byA.compose(ImageGrouping.byRegion)

      byAR.by("A", "01") === imagesA1
      byAR.by("A", "05") === imagesA2

      byAR.by("B", "01") === imagesB1
      byAR.by("B", "05") === imagesB2
    }
  }
}
