package org.scalawiki.wlx.stat

import org.scalawiki.dto.Image
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto.lists.WlmUa
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.scalawiki.wlx.{ImageDB, MonumentDB}
import org.specs2.mutable.Specification

class MostPopularMonumentsSpec extends Specification {

  val startYear = 2015
  val contest = Contest.WLMUkraine(2015)

  def monument(id: String, name: String) =
    new Monument(id = id, name = name, listConfig = Some(WlmUa))

  def monuments(n: Int, regionId: String, namePrefix: String, startId: Int = 1): Seq[Monument] =
    (startId until startId + n).map(i => monument(s"$regionId-xxx-000$i", namePrefix + i))

    "mostPopularMonuments" should {
      "work on no monuments" in {
        val monumentDb = new MonumentDB(contest, Seq.empty)
        val table = new MostPopularMonuments(Seq.empty, Some(new ImageDB(contest, Seq.empty, monumentDb)), monumentDb).table

        table.headers === Seq("Id", "Name", "0 years photos", "0 years authors")
        table.data.isEmpty === true
      }

      "work on no images" in {
        val output = new Output

        val monumentDb = new MonumentDB(contest,
          monuments(2, "01", "Crimea") ++
            monuments(5, "05", "Podillya") ++
            monuments(7, "07", "Volyn")
        )

        val table = new MostPopularMonuments(Seq.empty, Some(new ImageDB(contest, Seq.empty, monumentDb)), monumentDb).table

        table.headers === Seq("Id", "Name", "0 years photos", "0 years authors")
        table.data.isEmpty === true
      }

      "work with images" in {
        val images = Seq(
          Image("File:Img11.jpg", monumentId = Some("01-xxx-0001"), author = Some("FromCrimea")),
          Image("File:Img51.jpg", monumentId = Some("05-xxx-0001"), author = Some("FromPodillya1")),
          Image("File:Img52.jpg", monumentId = Some("05-xxx-0001"), author = Some("FromPodillya2")),
          Image("File:Img71.jpg", monumentId = Some("07-xxx-0001"), author = Some("FromVolyn")),
          Image("File:Img12.jpg", monumentId = Some("01-xxx-0001"), author = Some("FromCrimea"))
        )

        val monumentDb = new MonumentDB(contest,
          monuments(2, "01", "Crimea") ++
            monuments(5, "05", "Podillya") ++
            monuments(7, "07", "Volyn")
        )

        val table = new MostPopularMonuments(Seq.empty, Some(new ImageDB(contest, images, monumentDb)), monumentDb).table

        table.headers === Seq("Id", "Name", "0 years photos", "0 years authors")
        table.data === Seq(
          Seq("01-xxx-0001", "Crimea1", "2", "1"),
          Seq("05-xxx-0001", "Podillya1", "2", "2"),
          Seq("07-xxx-0001", "Volyn1", "1", "1")
        )
      }

    "work with images 2 years" in {
      val images1 = Seq(
        Image("File:Img11y1f1.jpg", monumentId = Some("01-xxx-0001"), author = Some("FromCrimea")),
        Image("File:Img11y1f2.jpg", monumentId = Some("01-xxx-0001"), author = Some("FromCrimea")),
        Image("File:Img51y1f1.jpg", monumentId = Some("05-xxx-0001"), author = Some("FromPodillya1")),
        Image("File:Img51y1f2.jpg", monumentId = Some("05-xxx-0001"), author = Some("FromPodillya2")),
        Image("File:Img71y1f1.jpg", monumentId = Some("07-xxx-0001"), author = Some("FromVolyn"))
      )

      val images2 = Seq(
        Image("File:Img11y2f1.jpg", monumentId = Some("01-xxx-0001"), author = Some("FromCrimea1")),
        Image("File:Img12y2f1.jpg", monumentId = Some("01-xxx-0002"), author = Some("FromCrimea2")),
        Image("File:Img52y2f1.jpg", monumentId = Some("05-xxx-0002"), author = Some("FromPodillya1")),
        Image("File:Img52y2f1.jpg", monumentId = Some("05-xxx-0002"), author = Some("FromPodillya2")),
        Image("File:Img72y2f1.jpg", monumentId = Some("07-xxx-0002"), author = Some("FromVolyn"))
      )

      val monumentDb = new MonumentDB(contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val table = new MostPopularMonuments(
        Seq(images1, images2).zipWithIndex.map { case (images, i) => new ImageDB(Contest.WLMUkraine(2014 + i), images, monumentDb) },
        Some(new ImageDB(contest, images1 ++ images2, monumentDb)),
        monumentDb).table

      table.headers === Seq("Id", "Name", "2 years photos", "2 years authors", "2014 photos", "2014 authors", "2015 photos", "2015 authors")
      table.data === Seq(
        Seq("01-xxx-0001", "Crimea1", "3", "2", "2", "1", "1", "1"),
        Seq("01-xxx-0002", "Crimea2", "1", "1", "0", "0", "1", "1"),
        Seq("05-xxx-0001", "Podillya1", "2", "2", "2", "2", "0", "0"),
        Seq("05-xxx-0002", "Podillya2", "2", "2", "0", "0", "2", "2"),
        Seq("07-xxx-0001", "Volyn1", "1", "1", "1", "1", "0", "0"),
        Seq("07-xxx-0002", "Volyn2", "1", "1", "0", "0", "1", "1")
      )
    }
  }
}
