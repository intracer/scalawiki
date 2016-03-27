package org.scalawiki.xml

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.specs2.matcher.ContentMatchers._
import org.specs2.mutable.Specification
import org.scalawiki.dto.markup.LineUtil._

class XmlIndexSpec extends Specification {
  "xml indexer" should {
    // TODO verify offsets with SkippingInputStream
    "create index" in {
      val parser = XmlHelper.parseExportDemo

      val pages = XmlIndex.fromParser(parser).toSeq

      pages.size === 3

      val p1 = pages(0)
      (p1.id, p1.title) === (1, "Page title")

      val p2 = pages(1)
      (p2.id, p2.title) === (2, "Talk:Page title")

      val p3 = pages(2)
      (p3.id, p3.title) === (3, "File:Some image.jpg")
    }

    "parse line" in {
      val pi = PageIndex.fromString("2208:1:Page title")
      (pi.offset, pi.id, pi.title) === (2208, 1, "Page title")
    }

    "parse line with namespace prefix" in {
      val pi = PageIndex.fromString("4522:2:Talk:Page title")
      (pi.offset, pi.id, pi.title) === (4522, 2, "Talk:Page title")
    }

    "format line" in {
      val pi = PageIndex(2208, 1, "Page title")
      pi.toString === "2208:1:Page title"
    }

    "format line with namespace prefix" in {
      val pi = PageIndex(4522, 2, "Talk:Page title")
      pi.toString === "4522:2:Talk:Page title"
    }

    "parse index" in {
      val index =
        """2208:1:Page title
          |4522:2:Talk:Page title
          |5004:3:File:Some image.jpg
          |""".stripMargin

      val pages = XmlIndex.fromInputStream(new ByteArrayInputStream(index.getBytes)).toSeq

      pages.size === 3

      val p1 = pages(0)
      (p1.offset, p1.id, p1.title) === (2208, 1, "Page title")

      val p2 = pages(1)
      (p2.offset, p2.id, p2.title) === (4522, 2, "Talk:Page title")

      val p3 = pages(2)
      (p3.offset, p3.id, p3.title) === (5004, 3, "File:Some image.jpg")
    }

    "format index" in {
      val p1 = PageIndex(2208, 1, "Page title")
      val p2 = PageIndex(4522, 2, "Talk:Page title")
      val p3 = PageIndex(5004, 3, "File:Some image.jpg")

      val pageIndex = new XmlIndex(Seq(p1, p2, p3))

      val os = new ByteArrayOutputStream

      pageIndex.save(os)

      val actual = os.toString

      val expected =
        """2208:1:Page title
          |4522:2:Talk:Page title
          |5004:3:File:Some image.jpg
          |""".stripMargin

      actual must haveSameLinesAs(expected)
    }

  }
}
