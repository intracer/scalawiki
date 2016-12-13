package org.scalawiki.bots

import TextLib._
import org.specs2.mutable.Specification

class ReplaceSpec extends Specification {

  "replaceExcept" should {
    "no replace" in {
      replaceExcept("12345678", "x", "y") === "12345678"
    }

    "simple replace" in {
      replaceExcept("AxB", "x", "y") === "AyB"
      replaceExcept("AxxB", "x", "y") === "AyyB"
      replaceExcept("AxyxB", "x", "y") === "AyyyB"
    }

    "regex replace" in {
      replaceExcept("A123B", "\\d", "x") === "AxxxB"
      replaceExcept("A123B", "\\d+", "x") === "AxB"
      replaceExcept("A123B", "A(\\d)2(\\d)B", "A$1x$2B") === "A1x3B"
      replaceExcept("", "(a?)", "$1B") === "B"
      replaceExcept("abc", "x*", "-") === "-a-b-c-"
      replaceExcept("", "(a)?", "$1$1") === ""
      replaceExcept("A123B", "A(?<a>\\d)2(?<b>\\d)B", "A${a}x${b}B") === "A1x3B"
      replaceExcept("A123B", "A(?<a>\\d)2(\\d)B", "A${a}x$2B") === "A1x3B"

      // test regex with lookbehind.
      replaceExcept("A behindB C", "(?<=behind)\\w", "Z") === "A behindZ C"

      // test regex with lookbehind and groups.
      replaceExcept("A behindB C D", "(?<=behind)\\w( )", "$1Z") === "A behind ZC D"

      // test regex with lookahead.
      replaceExcept("A Bahead C", "\\w(?=ahead)", "Z") === "A Zahead C"

      // test regex with lookahead and groups.
      replaceExcept("A Bahead C D", "( )\\w(?=ahead)", "Z$1") === "AZ ahead C D"
    }

    "case sensitivity" in {
      replaceExcept("AxB", "x", "y", ignoreCase = false) === "AyB"
      replaceExcept("AxB", "X", "y", ignoreCase = false) === "AxB"
      replaceExcept("AxB", "x", "y", ignoreCase = true) === "AyB"
      replaceExcept("AxB", "X", "y", ignoreCase = true) === "AyB"
    }

    "replace with marker" in {
      replaceExcept("AxyxB", "x", "y", marker = Some(".")) === "Ayyy.B"
      replaceExcept("AxyxB", "1", "y", marker = Some(".")) === "AxyxB."
    }

    "overlapping replace" in {
      replaceExcept("1111", "11", "21") === "2121"

      // allow overlap not supported
      //replaceExcept("1111", "11", "21") === "2221"
    }

    "replacing not inside a specific regex" in {
      replaceExcept("123x123", "123", "000") === "000x000"
      replaceExcept("123x123", "123", "000", exceptions = Seq("\\w123".r)) === "000x123"
    }
  }
}