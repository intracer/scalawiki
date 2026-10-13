package org.scalawiki.dto

import java.time.ZonedDateTime
import org.specs2.mutable.Specification

class PageListSpec extends Specification {
  "continued page properties" should {
    "retain image information delivered after the page and revision" in {
      val page = Page(Some(1L), Some(Namespace.FILE), "File:A.jpg")
        .withText("{{Monument Ukraine|123}}")
      val image = Image("File:A.jpg", date = Some(ZonedDateTime.parse("2012-09-01T00:00Z")))
      val pages = new PageList()
      pages.addPages(Seq(page))
      pages.addPages(Seq(page.copy(revisions = Nil, images = Seq(image))))

      val converted = pages.allPages.flatMap(Image.fromPage(Some("Monument Ukraine"))).toSeq
      converted must haveSize(1)
      converted.head.monumentIds must_== Seq("123")
      converted.head.date must_== image.date
    }

    "retain every continued image version with the latest version first" in {
      val latest = Image("File:A.jpg", date = Some(ZonedDateTime.parse("2026-10-13T00:00Z")))
      val older = latest.copy(date = Some(ZonedDateTime.parse("2012-09-01T00:00Z")))
      val page = Page(Some(1L), Some(Namespace.FILE), "File:A.jpg", images = Seq(latest))
      val pages = new PageList()
      pages.addPages(Seq(page))
      pages.addPages(Seq(page.copy(images = Seq(older))))
      pages.allPages.head.images must_== Seq(latest, older)
      Image.fromPageImages(pages.allPages.head).flatMap(_.date) must_== latest.date
    }
  }
}
