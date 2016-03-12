package org.scalawiki.wlx.stat

import org.scalawiki.dto.Image
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto.lists.WlmUa
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.scalawiki.wlx.{ImageDB, MonumentDB}
import org.specs2.mutable.Specification

class OutputSpec extends Specification {

  val contest = Contest.WLMUkraine(2015)

  def monument(id: String, name: String) =
    new Monument(id = id, name = name, listConfig = WlmUa)

  def monuments(n: Int, regionId: String, namePrefix: String, startId: Int = 1): Seq[Monument] =
    (startId to (startId + n - 1)).map(i => monument(s"$regionId-xxx-000$i", namePrefix + i))

  "monuments pictured" should {

    "no monuments" in {

      val output = new Output

      val monumentDb = new MonumentDB(contest, Seq.empty)
      val totalImageDb = new ImageDB(contest, Seq.empty, monumentDb)
      val imageDbs = Seq.empty

      val (images: String, table: Table) = output.monumentsPicturedTable(imageDbs, totalImageDb, monumentDb)

      table.headers === Seq("Region", "Objects in lists", "0 years total", "0 years percentage")
      table.data === Seq(
        Seq("Total", "0", "0", "0")
      )
    }

    "no monuments pictured" in {

      val output = new Output

      val monumentDb = new MonumentDB(contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val totalImageDb = new ImageDB(contest, Seq.empty, monumentDb)
      val imageDbs = Seq.empty

      val (images: String, table: Table) = output.monumentsPicturedTable(imageDbs, totalImageDb, monumentDb)

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

      val (images: String, table: Table) = output.monumentsPicturedTable(imageDbs, totalImageDb, monumentDb)

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

      val (imgStr: String, table: Table) = output.monumentsPicturedTable(imageDbs, totalImageDb, monumentDb)

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

      val (imgStr: String, table: Table) = output.monumentsPicturedTable(imageDbs, totalImageDb, monumentDb)

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

    "mostPopularMonuments" should {
      "work on no monuments" in {
        val output = new Output
        val monumentDb = new MonumentDB(contest, Seq.empty)
        val table = output.mostPopularMonumentsTable(Seq.empty, new ImageDB(contest, Seq.empty, monumentDb), monumentDb)

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

        val table = output.mostPopularMonumentsTable(Seq.empty, new ImageDB(contest, Seq.empty, monumentDb), monumentDb)

        table.headers === Seq("Id", "Name", "0 years photos", "0 years authors")
        table.data.isEmpty === true
      }

      "work with images" in {
        val output = new Output

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

        val table = output.mostPopularMonumentsTable(Seq.empty, new ImageDB(contest, images, monumentDb), monumentDb)

        table.headers === Seq("Id", "Name", "0 years photos", "0 years authors")
        table.data === Seq(
          Seq("01-xxx-0001", "Crimea1", "2", "1"),
          Seq("05-xxx-0001", "Podillya1", "2", "2"),
          Seq("07-xxx-0001", "Volyn1", "1", "1")
        )
      }
    }

    "work with images 2 years" in {
      val output = new Output

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

      val table = output.mostPopularMonumentsTable(
        Seq(images1, images2).zipWithIndex.map { case (images, i) => new ImageDB(Contest.WLMUkraine(2014 + i), images, monumentDb) },
        new ImageDB(contest, images1 ++ images2, monumentDb),
        monumentDb)

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

  "authorsContributed" should {
    "work on no monuments" in {
      val output = new Output
      val monumentDb = new MonumentDB(contest, Seq.empty)
      val table = output.authorsContributedTable(Seq.empty, new ImageDB(contest, Seq.empty, monumentDb), Some(monumentDb))

      table.headers === Seq("Region", "0 years total")
      table.data === Seq(Seq("Total", "0"))
    }

    "work on no images" in {
      val output = new Output

      val monumentDb = new MonumentDB(contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val table = output.authorsContributedTable(Seq.empty, new ImageDB(contest, Seq.empty, monumentDb), Some(monumentDb))

      table.headers === Seq("Region", "0 years total")
      table.data === Seq(
        Seq("Автономна Республіка Крим", "0"),
        Seq("Вінницька область", "0"),
        Seq("Волинська область", "0"),
        Seq("Total", "0")
      )
    }

    "work with images" in {
      val output = new Output

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

      val table = output.authorsContributedTable(Seq.empty, new ImageDB(contest, images, monumentDb), Some(monumentDb))

      table.headers === Seq("Region", "0 years total")
      table.data === Seq(
        Seq("Автономна Республіка Крим", "1"),
        Seq("Вінницька область", "2"),
        Seq("Волинська область", "1"),
        Seq("Total", "4")
      )
    }

    "work with images 2 years" in {
      val output = new Output

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

      val table = output.authorsContributedTable(
        Seq(images1, images2).zipWithIndex.map { case (images, i) => new ImageDB(Contest.WLMUkraine(2014 + i), images, monumentDb) },
        new ImageDB(contest, images1 ++ images2, monumentDb),
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
