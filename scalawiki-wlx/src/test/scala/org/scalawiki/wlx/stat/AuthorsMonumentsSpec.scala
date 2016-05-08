package org.scalawiki.wlx.stat

import org.scalawiki.dto.Image
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
  }

}