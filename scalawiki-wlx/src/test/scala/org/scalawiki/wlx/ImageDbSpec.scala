package org.scalawiki.wlx

import org.scalawiki.dto.{Image, User}
import org.scalawiki.wlx.dto._
import org.scalawiki.wlx.dto.lists.ListConfig
import org.specs2.mutable.Specification

class ImageDbSpec extends Specification {

  private val Ukraine = Country.Ukraine
  val monuments = Ukraine.regionIds.flatMap { regionId =>
    (1 to regionId.toInt).map { i =>
      Monument(
        page = "",
        id = regionId + "-001-" + f"$i%04d",
        name = "Monument in " + Ukraine.regionName(regionId),
        listConfig = Some(ListConfig.WlmUa)
      )
    }
  }

  def images(year: Int): Set[Image] = {
    var imageCount = 0
    Ukraine.regionIds.flatMap { regionId =>
      (1 to regionId.toInt).flatMap { i =>
        val id = regionId + "-001-" + f"$i%04d"

        if (i % 10 == year % 10 || i % 10 - 1 == year % 10) {
          imageCount += 1
          Some(
            Image(
              s"image of $id taken on $year number $imageCount",
              None,
              None,
              Some(10),
              Some(10),
              Some(10),
              Some(id),
              Some(User(None, Some("author")))
            )
          )
        } else None
      }
    }
  }

  val allImages = images(2012) ++ images(2013) ++ images(2014)
  val contest = Contest.WLMUkraine(2014)

//    "group images by regions" in {
//
//      val src = images(2014).toSeq
//
//      val monumentDb = new MonumentDB(contest, monuments.toSeq)
//
//      val db = new ImageDB(contest, src, monumentDb)
//
//      val regions = db._imagesByRegion.keySet
//
//      for (region <- regions) {
//        println(db._imagesByRegion(region).size)
//      }
//
//      1 === 1
//    }

  "by megapixel db" should {
    "show image list" in {
      val noRes = new Image("imageNoRes", width = None, height = None)

      val halfMinus =
        new Image("image_800_600", width = Some(800), height = Some(600))
      val halfPlus =
        new Image("image_1024_768", width = Some(1024), height = Some(768))
      val one =
        new Image("image_1000_1000", width = Some(1000), height = Some(1000))
      val onePlus =
        new Image("image_1280_1024", width = Some(1280), height = Some(1024))
      val twoPlus =
        new Image("image_1920_1200", width = Some(1900), height = Some(1200))
      val mp12 =
        new Image("image_12Mp", width = Some(4000), height = Some(3000))
      val mp24 =
        new Image("image_24Mp", width = Some(6000), height = Some(4000))

      val allImages =
        Seq(noRes, halfMinus, halfPlus, one, onePlus, twoPlus, mp12, mp24)

      val imageDb = new ImageDB(
        contest,
        allImages,
        Some(new MonumentDB(contest, Seq.empty))
      )

//      imageDb.byMegaPixels(None) === Seq(noRes)
      imageDb.byMegaPixels(0) === Seq(halfMinus, halfPlus)
      imageDb.byMegaPixels(1) === Seq(one, onePlus)
      imageDb.byMegaPixels(2) === Seq(twoPlus)
      imageDb.byMegaPixels(12) === Seq(mp12)
      imageDb.byMegaPixels(24) === Seq(mp24)
      imageDb.byMegaPixels(50) === Seq.empty
    }

    "show image authors" in {
      val user1 = User(1, "user1")
      val user2 = User(2, "user2")

      val noRes = new Image(
        "imageNoRes",
        width = None,
        height = None,
        uploader = Some(user1)
      )

      val halfMinus = new Image(
        "image_800_600",
        width = Some(800),
        height = Some(600),
        author = Some("user1"),
        uploader = Some(user1)
      )
      val halfPlus = new Image(
        "image_1024_768",
        width = Some(1024),
        height = Some(768),
        author = Some("user2"),
        uploader = Some(user2)
      )
      val one = new Image(
        "image_1000_1000",
        width = Some(1000),
        height = Some(1000),
        author = Some("user1"),
        uploader = Some(user1)
      )
      val onePlus = new Image(
        "image_1280_1024",
        width = Some(1280),
        height = Some(1024),
        author = Some("user2"),
        uploader = Some(user2)
      )
      val twoPlus = new Image(
        "image_1920_1200",
        width = Some(1900),
        height = Some(1200),
        author = Some("user1"),
        uploader = Some(user1)
      )
      val mp12 = new Image(
        "image_12Mp",
        width = Some(4000),
        height = Some(3000),
        author = Some("user1"),
        uploader = Some(user1)
      )
      val mp24 = new Image(
        "image_24Mp",
        width = Some(6000),
        height = Some(4000),
        author = Some("user1"),
        uploader = Some(user1)
      )

      val allImages =
        Seq(noRes, halfMinus, halfPlus, one, onePlus, twoPlus, mp12, mp24)

      val imageDb = new ImageDB(
        contest,
        allImages,
        Some(new MonumentDB(contest, Seq.empty))
      )

      imageDb._byMegaPixelsAndAuthor(0) === Map(
        "user1" -> Seq(halfMinus),
        "user2" -> Seq(halfPlus)
      )
      imageDb._byMegaPixelsAndAuthor(1) === Map(
        "user1" -> Seq(one),
        "user2" -> Seq(onePlus)
      )
      imageDb._byMegaPixelsAndAuthor(2) === Map("user1" -> Seq(twoPlus))

      imageDb.byMegaPixelFilterAuthorMap(_ < 2) === Map(
        "user1" -> Seq(halfMinus, one),
        "user2" -> Seq(halfPlus, onePlus)
      )
    }

  }
}
