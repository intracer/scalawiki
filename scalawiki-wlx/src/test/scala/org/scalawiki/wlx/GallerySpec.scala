package org.scalawiki.wlx

import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.scalawiki.wlx.stat.Output
import org.specs2.mutable.Specification

class GallerySpec extends Specification {

  val contest = Contest.WLMUkraine(2015)
  val uploadConfig = contest.uploadConfigs.head
  val listConfig = uploadConfig.listConfig

  "Gallery" should {
    "by monument id" in {
      val monument1 = Monument(id = "01-111-1111", name = "name1", photo = Some("Img1.jpg"), listConfig = listConfig)
      val monument2 = Monument(id = "05-111-1111", name = "name2", listConfig = listConfig)
      val monument3 = Monument(id = "05-111-1112", name = "name3", listConfig = listConfig)
      val monuments = Seq(monument1, monument2, monument3)
      val text = "header\n" + monuments.map(_.asWiki).mkString + "\nfooter"

      val images = Seq(
        Image("File:Img1.jpg", size = Some(10 ^ 6), width = Some(2048), height = Some(1024), monumentId = Some("01-111-1111")),
        Image("File:Img2.jpg", size = Some(10 ^ 6), width = Some(1280), height = Some(1024), monumentId = Some("05-111-1111")),
        Image("File:Img2sm.jpg", size = Some(10 ^ 6), width = Some(1024), height = Some(768), monumentId = Some("05-111-1111"))
      )
      val monumentDb = new MonumentDB(contest, monuments)
      val imageDb = new ImageDB(contest, images, Some(monumentDb))

      val expected =
        """== [[:uk:Вікіпедія:Вікі любить Землю/Автономна Республіка Крим|Автономна Республіка Крим]] ==
          |=== 01-111-1111 ===
          |name1
          |<gallery>
          |File:Img1.jpg
          |</gallery>
          |== [[:uk:Вікіпедія:Вікі любить Землю/Вінницька область|Вінницька область]] ==
          |=== 05-111-1111 ===
          |name2
          |<gallery>
          |File:Img2.jpg
          |File:Img2sm.jpg
          |</gallery>""".stripMargin

      val actual = new Output().galleryByRegionAndId(monumentDb, imageDb)
      // compare this way to work across different line endings
      actual.lines.toBuffer === expected.lines.toBuffer
    }


  }

}
