package org.scalawiki.wlx

import org.scalawiki.MwBot
import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.specs2.mutable.Specification

class ImageFillerSpec extends Specification {

  val contest = Contest.WLMUkraine(2015)
  val uploadConfig = contest.uploadConfigs.head
  val listConfig = uploadConfig.listConfig
  val host = MwBot.ukWiki

  "ListFiller" should {
    "select bestImage" in {
      val images = Seq(
        Image("10mp", size = Some(2 * 10 ^ 9), width = Some(5000), height = Some(2000)),
        Image("16mp", size = Some(3 * 10 ^ 9), width = Some(4000), height = Some(4000)),
        Image("16mpBigger", size = Some(4 * 10 ^ 9), width = Some(4000), height = Some(4000))
      )

      val best = ImageFiller.bestImage(images)
      best.title === "16mpBigger"
    }

    "addPhotosToPageText empty everything" in {

      val monumentDb = new MonumentDB(contest, Seq.empty)
      val imageDb = new ImageDB(contest, Seq.empty, Some(monumentDb))
      val task = new ListUpdaterTask(host, monumentDb, new ImageFillerUpdater(imageDb))

      val (newText, comment) = task.updatePage("page", "")
      newText === ""
      comment === "updated 0 monument(s)"
    }

    "addPhotosToPageText preserve list page" in {
      val monuments = Seq(
        Monument(id = "id1", name = "name1", listConfig = Some(listConfig)),
        Monument(id = "id2", name = "name2", listConfig = Some(listConfig)),
        Monument(id = "id3", name = "name3", listConfig = Some(listConfig))
      )
      val text = "header\n" + monuments.map(_.asWiki()).mkString("{|\n|}\n") + "\nfooter"

      val monumentDb = new MonumentDB(contest, monuments)
      val imageDb = new ImageDB(contest, Seq.empty, monumentDb)
      val task = new ListUpdaterTask(host, monumentDb, new ImageFillerUpdater(imageDb))

      val (newText, comment) = task.updatePage("page", text)
      newText === text
      comment === "updated 0 monument(s)"
    }

    "addPhotosToPageText add 1 image" in {
      val monument1 = Monument(id = "id1", name = "name1", photo = Some("Img1.jpg"), listConfig = Some(listConfig))
      val monument2 = Monument(id = "id2", name = "name2", listConfig = Some(listConfig))
      val monument3 = Monument(id = "id3", name = "name3", listConfig = Some(listConfig))
      val monuments = Seq(monument1, monument2, monument3)
      val text = "header\n" + monuments.map(_.asWiki()).mkString + "\nfooter"

      val images = Seq(
        Image("File:Img1.jpg", size = Some(10 ^ 6), width = Some(2048), height = Some(1024), monumentIds = List("id1")),
        Image("File:Img2.jpg", size = Some(10 ^ 6), width = Some(1280), height = Some(1024), monumentIds = List("id2")),
        Image("File:Img2sm.jpg", size = Some(10 ^ 6), width = Some(1024), height = Some(768), monumentIds = List("id2"))
      )
      val monumentDb = new MonumentDB(contest, monuments)
      val imageDb = new ImageDB(contest, images, monumentDb)
      val task = new ListUpdaterTask(host, monumentDb, new ImageFillerUpdater(imageDb))

      val (newText, comment) = task.updatePage("page", text)
      val updatedMonuments = Seq(
        monument1,
        monument2.copy(photo = Some("Img2.jpg")),
        monument3
      )
      val expected = "header\n" + updatedMonuments.map(_.asWiki()).mkString + "\nfooter"
      newText === expected
      comment === "updated 1 monument(s)"
    }

    "addPhotosToPageText add 1 image preserve comments" in {
      val monument1 = Monument(id = "id1", name = "name1", photo = Some("Img1.jpg"), listConfig = Some(listConfig))
      val monument2 = Monument(id = "id2", name = "name2", listConfig = Some(listConfig))
      val monument3 = Monument(id = "id3", name = "name3", listConfig = Some(listConfig))
      val monuments = Seq(monument1, monument2, monument3)
      val text = "header\n" + Seq("<!-- " + monument1.asWiki() + " --> ",
        monument2.asWiki(),
        monument3.asWiki()
      ).mkString + "\nfooter"

      val images = Seq(
        Image("File:Img1.jpg", size = Some(10 ^ 6), width = Some(2048), height = Some(1024), monumentIds = List("id1")),
        Image("File:Img2.jpg", size = Some(10 ^ 6), width = Some(1280), height = Some(1024), monumentIds = List("id2")),
        Image("File:Img2sm.jpg", size = Some(10 ^ 6), width = Some(1024), height = Some(768), monumentIds = List("id2"))
      )
      val monumentDb = new MonumentDB(contest, monuments)
      val imageDb = new ImageDB(contest, images, monumentDb)
      val task = new ListUpdaterTask(host, monumentDb, new ImageFillerUpdater(imageDb))

      val (newText, comment) = task.updatePage("page", text)

      val expected = "header\n" + Seq("<!-- " + monument1.asWiki() + " --> ",
        monument2.copy(photo = Some("Img2.jpg")).asWiki(),
        monument3.asWiki()
      ).mkString + "\nfooter"
      newText === expected
      comment === "updated 1 monument(s)"
    }

    "addPhotosToPageText should preserve surrounding markup" in {
      val monument1 = Monument(id = "id1", name = "name1", listConfig = Some(listConfig))
      val monument2 = Monument(id = "id2", name = "name2", listConfig = Some(listConfig))
      val monument3 = Monument(id = "id3", name = "name3", listConfig = Some(listConfig))
      val monuments = Seq(monument1, monument2, monument3)
      val text = "header\n" + monuments.map(_.asWiki()).mkString("{|\n|}\n") + "\nfooter"

      val images = Seq(
        Image("File:Img1.jpg", size = Some(10 ^ 6), width = Some(2048), height = Some(1024), monumentIds = List("id1")),
        Image("File:Img2.jpg", size = Some(10 ^ 6), width = Some(1280), height = Some(1024), monumentIds = List("id2")),
        Image("File:Img3.jpg", size = Some(10 ^ 6), width = Some(1024), height = Some(768), monumentIds = List("id3"))
      )
      val monumentDb = new MonumentDB(contest, monuments)
      val imageDb = new ImageDB(contest, images, monumentDb)

      val task = new ListUpdaterTask(host, monumentDb, new ImageFillerUpdater(imageDb))
      val (newText, comment) = task.updatePage("page", text)

      val updatedMonuments = Seq(
        monument1.copy(photo = Some("Img1.jpg")),
        monument2.copy(photo = Some("Img2.jpg")),
        monument3.copy(photo = Some("Img3.jpg"))
      )
      val expected = "header\n" + updatedMonuments.map(_.asWiki()).mkString("{|\n|}\n") + "\nfooter"
      newText === expected
      comment === "updated 3 monument(s)"
    }

    "addPhotosToPageText should not add localized File:" in {
      val images = Seq(
        Image("Файл:Image1.jpg", monumentIds = List("id1"))
      )

      val monument1 = Monument(id = "id1", name = "name1", listConfig = Some(listConfig))
      val monuments = Seq(monument1)
      val monumentDb = new MonumentDB(contest, monuments)

      val imageDb = new ImageDB(contest, images, monumentDb)
      val text = monuments.map(_.asWiki()).mkString
      val task = new ListUpdaterTask(host, monumentDb, new ImageFillerUpdater(imageDb))

      val (newText, comment) = task.updatePage("page", text)
      val updatedMonuments = Seq(
        monument1.copy(photo = Some("Image1.jpg"))
      )
      val expected = updatedMonuments.map(_.asWiki()).mkString
      newText === expected
      comment === "updated 1 monument(s)"
    }
  }
}
