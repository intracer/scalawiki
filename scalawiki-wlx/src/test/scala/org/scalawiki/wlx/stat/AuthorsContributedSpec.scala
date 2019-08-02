package org.scalawiki.wlx.stat

import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.lists.ListConfig.WlmUa
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.scalawiki.wlx.{ImageDB, MonumentDB}
import org.specs2.mutable.Specification

class AuthorsContributedSpec extends Specification {

  val contest = Contest.WLMUkraine(2015)

  def monument(id: String, name: String) =
    new Monument(id = id, name = name, listConfig = Some(WlmUa))

  def monuments(n: Int, regionId: String, namePrefix: String, startId: Int = 1): Seq[Monument] =
    (startId until startId + n).map(i => monument(s"$regionId-xxx-000$i", namePrefix + i))

  "authorsContributed" should {
    "work on no monuments" in {
      val output = new AuthorsStat
      val monumentDb = new MonumentDB(contest, Seq.empty)
      val table = output.authorsContributedTable(Seq.empty, Some(new ImageDB(contest, Seq.empty, monumentDb)), Some(monumentDb))

      table.headers === Seq("Region", "0 years total")
      table.data === Seq(Seq("Total", "0"))
    }

    "work on no images" in {
      val output = new AuthorsStat

      val monumentDb = new MonumentDB(contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val table = output.authorsContributedTable(Seq.empty, Some(new ImageDB(contest, Seq.empty, monumentDb)), Some(monumentDb))

      table.headers === Seq("Region", "0 years total")
      table.data === Seq(
        Seq("Автономна Республіка Крим", "0"),
        Seq("Вінницька область", "0"),
        Seq("Волинська область", "0"),
        Seq("Total", "0")
      )
    }

    "work with images" in {
      val output = new AuthorsStat

      val images = Seq(
        Image("File:Img11.jpg", monumentIds = List("01-xxx-0001"), author = Some("FromCrimea")),
        Image("File:Img51.jpg", monumentIds = List("05-xxx-0001"), author = Some("FromPodillya1")),
        Image("File:Img52.jpg", monumentIds = List("05-xxx-0001"), author = Some("FromPodillya2")),
        Image("File:Img71.jpg", monumentIds = List("07-xxx-0001"), author = Some("FromVolyn")),
        Image("File:Img12.jpg", monumentIds = List("01-xxx-0001"), author = Some("FromCrimea"))
      )

      val monumentDb = new MonumentDB(contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val table = output.authorsContributedTable(Seq.empty, Some(new ImageDB(contest, images, monumentDb)), Some(monumentDb))

      table.headers === Seq("Region", "0 years total")
      table.data === Seq(
        Seq("Автономна Республіка Крим", "1"),
        Seq("Вінницька область", "2"),
        Seq("Волинська область", "1"),
        Seq("Total", "4")
      )
    }

    "work with images 2 years" in {
      val output = new AuthorsStat

      val images1 = Seq(
        Image("File:Img11y1f1.jpg", monumentIds = List("01-xxx-0001"), author = Some("FromCrimea")),
        Image("File:Img11y1f2.jpg", monumentIds = List("01-xxx-0001"), author = Some("FromCrimea")),
        Image("File:Img51y1f1.jpg", monumentIds = List("05-xxx-0001"), author = Some("FromPodillya1")),
        Image("File:Img51y1f2.jpg", monumentIds = List("05-xxx-0001"), author = Some("FromPodillya2")),
        Image("File:Img71y1f1.jpg", monumentIds = List("07-xxx-0001"), author = Some("FromVolyn"))
      )

      val images2 = Seq(
        Image("File:Img11y2f1.jpg", monumentIds = List("01-xxx-0001"), author = Some("FromCrimea1")),
        Image("File:Img12y2f1.jpg", monumentIds = List("01-xxx-0002"), author = Some("FromCrimea2")),
        Image("File:Img52y2f1.jpg", monumentIds = List("05-xxx-0002"), author = Some("FromPodillya1")),
        Image("File:Img52y2f1.jpg", monumentIds = List("05-xxx-0002"), author = Some("FromPodillya2")),
        Image("File:Img72y2f1.jpg", monumentIds = List("07-xxx-0002"), author = Some("FromVolyn"))
      )

      val monumentDb = new MonumentDB(contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val table = output.authorsContributedTable(
        Seq(images1, images2).zipWithIndex.map { case (images, i) => new ImageDB(Contest.WLMUkraine(2014 + i), images, monumentDb) },
        Some(new ImageDB(contest, images1 ++ images2, monumentDb)),
        Some(monumentDb))

      table.headers === Seq("Region", "2 years total", "2014", "2015")
      table.data === Seq(
        Seq("Автономна Республіка Крим", "3", "1", "2"),
        Seq("Вінницька область", "2", "2", "2"),
        Seq("Волинська область", "1", "1", "1"),
        Seq("Total", "6", "4", "5")
      )
    }
  }


}
