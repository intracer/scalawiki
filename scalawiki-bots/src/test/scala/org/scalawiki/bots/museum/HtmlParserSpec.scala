package org.scalawiki.bots.museum

import org.specs2.matcher.ContentMatchers._
import org.scalawiki.dto.markup.LineUtil._
import org.specs2.mutable.Specification
import org.scalawiki.util.TestUtils._

import scala.compat.Platform

class HtmlParserSpec extends Specification {

  "HtmlParser" should {
    "get list of images" in {
      val s = resourceAsString("/org/scalawiki/bots/museum/imageList.html")

      val lines = HtmlParser.trimmedLines(s)
      lines === Seq(
        "Image descriptions",
        "Image list",
        "1. Archaeological Museum. Exterior.",
        "2. Archaeological Museum. Exposition."
      )
    }

    "htmlText" should {
      "join new lines" in {
        HtmlParser.htmlText(""" line1
            | line2
            |
            | line3
          """.stripMargin) === "line1 line2 line3"
      }

      "preserve html paragraphs" in {
        HtmlParser
          .htmlText("""<p>line1
            |line2</p>
            |<p>line3</p>
          """.stripMargin)
          .split("\r?\n")
          .map(_.trim) ===
          """line1 line2
            |line3""".stripMargin.split("\r?\n").map(_.trim)

      }
    }
  }
}
