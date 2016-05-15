package org.scalawiki.wlx.stat

import org.scalawiki.dto.Image
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto._
import org.scalawiki.wlx.{ImageDB, MonumentDB}
import org.specs2.mutable.Specification

class AuthorsMonumentsSpec extends Specification {

  val contest = new Contest(ContestType.WLE, Country.Ukraine, 2013, uploadConfigs = Seq.empty[UploadConfig])

  def getTable(images: Seq[Image], monuments: Seq[Monument], contest: Contest = contest): Table = {
    val mdb = Some(new MonumentDB(contest, monuments))

    val db = new ImageDB(contest, images, mdb, mdb)

    new Output().authorsMonumentsTable(db)
  }

  "stat" should {
    "be empty without regions" in {
      val noRegions = contest.copy(country = Country.Azerbaijan)
      val table = getTable(Seq.empty[Image], Seq.empty[Monument], noRegions)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded")

      table.data === Seq(
        Seq("Total") ++ Seq.fill(2)("0")
      )
    }

    "be empty with regions" in {
      val images = Seq.empty[Image]
      val monuments = Seq.empty[Monument]
      val table = getTable(images, monuments)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded") ++ contest.country.regionNames

      table.data === Seq(
        Seq("Total") ++ Seq.fill(2 + contest.country.regions.size)("0")
      )
    }

    "have 1 unknown image" in {
      val images = Seq(Image("image1.jpg"))
      val monuments = Seq.empty[Monument]
      val table = getTable(images, monuments)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded") ++ contest.country.regionNames

      table.data === Seq(
        Seq("Total", "0", "1") ++ Seq.fill(contest.country.regions.size)("0")
      )
    }

    "have 1 image with author" in {
      val user = "user"
      val images = Seq(Image("image1.jpg", author = Some(user)))
      val monuments = Seq.empty[Monument]
      val table: Table = getTable(images, monuments)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded") ++ contest.country.regionNames

      // TODO why no author???
      table.data === Seq(
        Seq("Total", "0", "1") ++ Seq.fill(contest.country.regions.size)("0")
      )
    }

    "have 1 image with author and monument with undefined regions" in {
      val user = "user"
      val images = Seq(Image("image1.jpg", author = Some(user), monumentId = Some("123")))
      val monuments = Seq( new Monument(id = "123", name = "123 monument"))

      val table = getTable(images, monuments)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded") ++ contest.country.regionNames

      table.data === Seq(
        Seq("Total", "1", "1") ++ Seq.fill(contest.country.regions.size)("0"),
        Seq("[[User:user|user]]", "1", "1") ++ Seq.fill(contest.country.regions.size)("0")
      )
    }

    "have 1 image with author and monument no regions" in {
      val noRegions = contest.copy(country = Country.Azerbaijan)
      val user = "user"
      val images = Seq(Image("image1.jpg", author = Some(user), monumentId = Some("123")))
      val monuments = Seq( new Monument(id = "123", name = "123 monument"))

      val table = getTable(images, monuments, noRegions)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded")

      table.data === Seq(
        Seq("Total", "1", "1"),
        Seq("[[User:user|user]]", "1", "1")
      )
    }
  }
}