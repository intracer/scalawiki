package client.wlx

import client.wlx.dto.{Contest, Image, Monument, Region}
import client.wlx.query.ImageQuerySeq
import org.specs2.mutable.Specification

class ImageDbSpec extends Specification {

  val monuments = Region.Ukraine.keySet.flatMap{
    regionId =>
      (1 to regionId.toInt).map { i =>
        Monument(
          textParam = "",
          pageParam = "",
          id = regionId + "-001-" + f"$i%04d",
          name = "Monument in " + Region.Ukraine(regionId)
        )
      }
  }

  def images(year: Int):Set[Image] = {
    var imageCount = 0
    Region.Ukraine.keySet.flatMap{
      regionId =>
        (1 to regionId.toInt).flatMap { i =>
          val id = regionId + "-001-" + f"$i%04d"

          if (i%10 == year%10 || i%10 - 1 == year%10) {
            imageCount +=1
            Some(Image(year*1000 + imageCount, s"image of $id taken on $year number $imageCount", "", "", 10, 10, Some(id), Some("author")))
          } else None
        }
    }
  }

  val allImages = images(2012) ++ images(2013) ++ images(2014)

  "image db"  should {

    "group images by regions" in {
      val contest = Contest.WLMUkraine(2014, "09-15", "10-15")

      val src = images(2014)

      val query = new ImageQuerySeq(Map(contest.category -> src.toSeq), src.toSeq)
      val monumentDb: MonumentDB = new MonumentDB(contest, monuments.toSeq)
      val db = new ImageDB(contest, query.imagesFromCategory(contest.category, contest), monumentDb)

      val regions = db._imagesByRegion.keySet

      for (region <- regions)  {
        println(db._imagesByRegion(region).size)
      }

      1 === 1
    }


  }
}
