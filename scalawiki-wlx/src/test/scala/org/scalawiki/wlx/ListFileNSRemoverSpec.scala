package org.scalawiki.wlx

import org.scalawiki.MwBot
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.specs2.mutable.Specification

class ListFileNSRemoverSpec extends Specification {

  val contest = Contest.WLMUkraine(2015)
  val uploadConfig = contest.uploadConfigs.head
  val listConfig = uploadConfig.listConfig
  val host = MwBot.ukWiki

  "ListFileNSRemover" should {

    "work with empty everything" in {

      val monumentDb = new MonumentDB(contest, Seq.empty)
      val task = new ListFileNSRemoverTask(host, monumentDb)

      val (newText, comment) = task.updatePage("page", "")
      newText === ""
      comment === "fixing 0 image(s)"
    }

    "preserve list page" in {
      val monuments = Seq(
        Monument(id = "id1", name = "name1", listConfig = Some(listConfig)),
        Monument(id = "id2", name = "name2", listConfig = Some(listConfig)),
        Monument(id = "id3", name = "name3", listConfig = Some(listConfig))
      )
      val text = "header\n" + monuments.map(_.asWiki()).mkString("{|\n|}\n") + "\nfooter"

      val monumentDb = new MonumentDB(contest, Seq.empty)
      val task = new ListFileNSRemoverTask(host, monumentDb)

      val (newText, comment) = task.updatePage("page", text)
      newText === text
      comment === "fixing 0 image(s)"
    }

    "fix 1 image" in {
      val monument1 = Monument(id = "id1", name = "name1", photo = Some("File:Img1.jpg"), listConfig = Some(listConfig))
      val monument2 = Monument(id = "id2", name = "name2", listConfig = Some(listConfig))
      val monument3 = Monument(id = "id3", name = "name3", listConfig = Some(listConfig))
      val monuments = Seq(monument1, monument2, monument3)
      val text = "header\n" + monuments.map(_.asWiki()).mkString + "\nfooter"

      val monumentDb = new MonumentDB(contest, Seq.empty)
      val task = new ListFileNSRemoverTask(host, monumentDb)

      val (newText, comment) = task.updatePage("page", text)
      val updatedMonuments = Seq(
      monument1.copy(photo = Some("Img1.jpg")),
      monument2,
      monument3
      )
      val expected = "header\n" + updatedMonuments.map(_.asWiki()).mkString + "\nfooter"
      newText === expected
      comment === "fixing 1 image(s)"
    }

    "should preserve surrounding markup" in {
      val monument1 = Monument(id = "id1", name = "name1", photo = Some("File:Img1.jpg"), listConfig = Some(listConfig))
      val monument2 = Monument(id = "id2", name = "name2", photo = Some("File:Img2.jpg"), listConfig = Some(listConfig))
      val monument3 = Monument(id = "id3", name = "name3", photo = Some("File:Img3.jpg"), listConfig = Some(listConfig))
      val monuments = Seq(monument1, monument2, monument3)
      val text = "header\n" + monuments.map(_.asWiki()).mkString("{|\n|}\n") + "\nfooter"

      val monumentDb = new MonumentDB(contest, monuments)

      val task = new ListFileNSRemoverTask(host, monumentDb)
      val (newText, comment) = task.updatePage("page", text)

      val updatedMonuments = Seq(
        monument1.copy(photo = Some("Img1.jpg")),
        monument2.copy(photo = Some("Img2.jpg")),
        monument3.copy(photo = Some("Img3.jpg"))
      )
      val expected = "header\n" + updatedMonuments.map(_.asWiki()).mkString("{|\n|}\n") + "\nfooter"
      newText === expected
      comment === "fixing 3 image(s)"
    }


    "fix localized File:" in {

      val monument1 = Monument(id = "id1", name = "name1", photo = Some("Файл:Img1.jpg"), listConfig = Some(listConfig))
      val monuments = Seq(monument1)
      val monumentDb = new MonumentDB(contest, monuments)

      val text = monuments.map(_.asWiki()).mkString
      val task = new ListFileNSRemoverTask(host, monumentDb)

      val (newText, comment) = task.updatePage("page", text)
      val updatedMonuments = Seq(
        monument1.copy(photo = Some("Img1.jpg"))
      )
      val expected = updatedMonuments.map(_.asWiki()).mkString
      newText === expected
      comment === "fixing 1 image(s)"
    }

  }
}
