package org.scalawiki.bots.museum

import org.specs2.mutable.Specification

import scala.io.Source

class HtmlParserSpec extends Specification {

  "HtmlParser" should {
    "get list of images" in {
      val is = getClass.getResourceAsStream("/org/scalawiki/bots/museum/imageList.html")
      is !== null
      val s = Source.fromInputStream(is).mkString

      val lines = HtmlParser.trimmedLines(s)
      lines === Seq(
        "Image descriptions",
        "Image list",
        "1. Archaeological Museum. Exterior.",
        "2. Archaeological Museum. Exposition."
      )
    }
  }
}