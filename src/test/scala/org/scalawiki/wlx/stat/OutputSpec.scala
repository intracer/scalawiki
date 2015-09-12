package org.scalawiki.wlx.stat

import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto.lists.WlmUa
import org.scalawiki.wlx.dto.{Image, Contest, Monument}
import org.scalawiki.wlx.{ImageDB, MonumentDB}
import org.specs2.mutable.Specification

class OutputSpec extends Specification {

  val contest = Contest.WLMUkraine(2015)

  def monument(id: String, name: String) =
    new Monument(id = id, name = name, listConfig = WlmUa)

  def monuments(n: Int, regionId: String, namePrefix: String, startId: Int = 1): Seq[Monument] =
    (startId to (startId + n - 1)).map(i => monument(s"$regionId-xxx-000$i", namePrefix + i))

  "OutputTest" should {
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

      val image = Image("File:Img3.jpg", size = Some(10^6), width = Some(1024), height = Some(768),
        monumentId = Some("01-xxx-0001"))

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

  }
}
