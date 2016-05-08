package org.scalawiki.wlx.stat

import org.scalawiki.dto.{Image, User}
import org.scalawiki.wlx.{ImageDB, MonumentDB}
import org.scalawiki.wlx.dto._
import org.specs2.mutable.Specification

class AuthorsMonumentsSpec extends Specification {

  val contest = new Contest(ContestType.WLE, Country.Ukraine, 2013, uploadConfigs = Seq.empty[UploadConfig])

  "stat" should {
    "be empty" in {
      val images = Seq.empty[Image]
      val monuments = Seq.empty[Monument]
      val mdb = Some(new MonumentDB(contest, monuments))

      val db = new ImageDB(contest, images, mdb, mdb)

      val table = new Output().authorsMonumentsTable(db)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded") ++ contest.country.regionNames

      table.data === Seq(
        Seq("Total") ++ Seq.fill(2 + contest.country.regions.size)("0")
      )
    }

    "have 1 unknown image" in {
      val images = Seq(Image("image1.jpg"))
      val monuments = Seq.empty[Monument]
      val mdb = Some(new MonumentDB(contest, monuments))

      val db = new ImageDB(contest, images, mdb, mdb)

      val table = new Output().authorsMonumentsTable(db)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded") ++ contest.country.regionNames

      table.data === Seq(
        Seq("Total", "0", "1") ++ Seq.fill(contest.country.regions.size)("0")
      )
    }

    "have 1 image with author" in {
      val user = "user"
      val images = Seq(Image("image1.jpg", author = Some(user)))
      val monuments = Seq.empty[Monument]
      val mdb = Some(new MonumentDB(contest, monuments))

      val db = new ImageDB(contest, images, mdb, mdb)

      val table = new Output().authorsMonumentsTable(db)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded") ++ contest.country.regionNames

      // TODO why no author???
      table.data === Seq(
        Seq("Total", "0", "1") ++ Seq.fill(contest.country.regions.size)("0")
      )
    }

    "have 1 image with author and monument" in {
      val user = "user"
      val images = Seq(Image("image1.jpg", author = Some(user), monumentId = Some("123")))
      val monuments = Seq( new Monument(id = "123", name = "123 monument"))
      val mdb = Some(new MonumentDB(contest, monuments))

      val db = new ImageDB(contest, images, mdb, mdb)

      val table = new Output().authorsMonumentsTable(db)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded") ++ contest.country.regionNames

      table.data === Seq(
        Seq("Total", "1", "1") ++ Seq.fill(contest.country.regions.size)("0"),
        Seq("[[User:user|user]]", "1", "1") ++ Seq.fill(contest.country.regions.size)("0")
      )
    }


  }

}