package org.scalawiki.dto.markup

import org.specs2.text.LinesContent

object LineUtil {

  implicit def linesForString: LinesContent[String] = new LinesContent[String] {
    def name (s: String) = "lines of text"
    def lines (s: String): Seq[String] = s.split("\r?\n")
  }

}
