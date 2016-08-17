package org.scalawiki.wlx.stat

import org.scalawiki.dto.Image
import org.scalawiki.wlx.{ImageDB, MonumentDB}
import org.scalawiki.wlx.dto.lists.WlmUa
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.specs2.mutable.Specification

class MonumentsPicturedByRegionSpec extends Specification {

  val startYear = 2015
  val contest = Contest.WLMUkraine(2015)

  def monument(id: String, name: String) =
    new Monument(id = id, name = name, listConfig = Some(WlmUa))

  def monuments(n: Int, regionId: String, namePrefix: String, startId: Int = 1): Seq[Monument] =
    (startId until startId + n).map(i => monument(s"$regionId-xxx-000$i", namePrefix + i))

  def getStat(monuments: Seq[Monument], images: Seq[Seq[Image]] = Seq(Seq.empty)) = {
    val monumentDb = new MonumentDB(contest, monuments)

    def imageDb(images: Seq[Image]) = new ImageDB(contest, images, monumentDb)

    val imagesDbs = images.map(imageDb)

    new ContestStat(contest, startYear,
      Some(monumentDb),
      imagesDbs.last,
      Some(imageDb(images.flatten)),
      imagesDbs.init)
  }

  "monuments pictured" should {

    "no monuments" in {
      val stat = getStat(Seq.empty)
      val table = new MonumentsPicturedByRegion(stat).table

      table.headers === Seq("Region", "Objects in lists", "0 years total", "0 years percentage")
      table.data === Seq(
        Seq("Total", "0", "0", "0")
      )
    }

    "no monuments pictured" in {
      val _monuments =
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")

      val stat = getStat(_monuments)

      val table = new MonumentsPicturedByRegion(stat).table

      table.headers === Seq("Region", "Objects in lists", "0 years total", "0 years percentage")
      table.data === Seq(
        Seq("Автономна Республіка Крим", "2", "0", "0"),
        Seq("Вінницька область", "5", "0", "0"),
        Seq("Волинська область", "7", "0", "0"),
        Seq("Total", "14", "0", "0")
      )
    }

    "monuments pictured" in {

      val output = new Output

      val monumentDb = new MonumentDB(contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val image = Image("File:Img3.jpg", monumentId = Some("01-xxx-0001"))

      val totalImageDb = new ImageDB(contest, Seq(image), monumentDb)
      val imageDbs = Seq.empty

      val table = new MonumentsPicturedByRegion(imageDbs, Some(totalImageDb), monumentDb).table

      table.headers === Seq("Region", "Objects in lists", "0 years total", "0 years percentage")
      table.data === Seq(
        Seq("Автономна Республіка Крим", "2", "1", "50"),
        Seq("Вінницька область", "5", "0", "0"),
        Seq("Волинська область", "7", "0", "0"),
        Seq("Total", "14", "1", "7")
      )
    }

    "two years monuments pictured" in {

      val output = new Output

      val monumentDb = new MonumentDB(contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val images = Seq(
        Image("File:Img1.jpg", monumentId = Some("01-xxx-0001")),
        Image("File:Img2.jpg", monumentId = Some("05-xxx-0001"))
      )

      val totalImageDb = new ImageDB(contest, images, monumentDb)
      val imageDbs = images.zipWithIndex.map { case (img, i) => new ImageDB(Contest.WLMUkraine(2014 + i), Seq(img), monumentDb) }

      val table = new MonumentsPicturedByRegion(imageDbs, Some(totalImageDb), monumentDb).table

      table.headers === Seq("Region", "Objects in lists", "2 years total", "2 years percentage",
        "2014 Objects", "2014 Pictures", "2015 Objects", "2015 Pictures")
      table.data === Seq(
        Seq("Автономна Республіка Крим", "2", "1", "50", "1", "1", "0", "0"),
        Seq("Вінницька область", "5", "1", "20", "0", "0", "1", "1"),
        Seq("Волинська область", "7", "0", "0", "0", "0", "0", "0"),
        Seq("Total", "14", "2", "14", "1", "1", "1", "1")
      )
    }

    "4 years monuments pictured" in {
      val output = new Output

      val monumentDb = new MonumentDB(contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val images = Seq(
        Image("File:Img1.jpg", monumentId = Some("01-xxx-0001")),
        Image("File:Img2.jpg", monumentId = Some("05-xxx-0001")),
        Image("File:Img3.jpg", monumentId = Some("07-xxx-0001")),
        Image("File:Img4.jpg", monumentId = Some("01-xxx-0002"))
      )

      val totalImageDb = new ImageDB(contest, images, monumentDb)
      val imageDbs = images.zipWithIndex.map { case (img, i) => new ImageDB(Contest.WLMUkraine(2012 + i), Seq(img), monumentDb) }

      val table = new MonumentsPicturedByRegion(imageDbs, Some(totalImageDb), monumentDb).table

      table.headers === Seq("Region", "Objects in lists", "4 years total", "4 years percentage",
        "2012 Objects", "2012 Pictures",
        "2013 Objects", "2013 Pictures",
        "2014 Objects", "2014 Pictures",
        "2015 Objects", "2015 Pictures"
      )
      val data = table.data.toSeq
      data.size === 4

      data(0) === Seq("Автономна Республіка Крим",
        "2", "2", "100", "1", "1", "0", "0", "0", "0", "1", "1")
      data(1) === Seq("Вінницька область",
        "5", "1", "20", "0", "0", "1", "1", "0", "0", "0", "0")
      data(2) === Seq("Волинська область",
        "7", "1", "14", "0", "0", "0", "0", "1", "1", "0", "0")
      data(3) === Seq("Total",
        "14", "4", "28", "1", "1", "1", "1", "1", "1", "1", "1")

    }
  }
}
