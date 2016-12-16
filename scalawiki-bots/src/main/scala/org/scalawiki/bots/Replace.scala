package org.scalawiki.bots

import java.util.regex.Pattern

import scala.util.matching.{RegexFactory, Regex}

class ReplacementBase(old: String,
                      replacement: String,
                      summary: Option[String] = None,
                      useRegex: Boolean = true,
                      ignoreCase: Boolean = false) {

  val flags = (if (!useRegex) Pattern.LITERAL else 0) |
    (if (ignoreCase) Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE else 0)

  val regex = RegexFactory.regex(Pattern.compile(old, flags))

  def description = s"-$old +$replacement"

}

case class Exceptions(titles: Seq[String],
                      requireTitles: Seq[String],
                      textContains: Seq[String],
                      inside: Seq[String],
                      insideTags: Seq[String]) {

  val (titlesR, requireTitlesR, textContainsR, insideR) = (
    titles.map(_.r),
    requireTitles.map(_.r),
    textContains.map(_.r),
    inside.map(_.r))

  def isTitleExcepted(title: String): Boolean =
    titlesR.exists(_.unapplySeq(title).isDefined) ||
      !requireTitlesR.exists(_.unapplySeq(title).isDefined)

  def isTextExcepted(text: String): Boolean =
    textContainsR.exists(_.findFirstIn(text).isDefined)

}

case class ReplaceConfig(regex: Boolean = false,
                         replacements: Seq[String] = Seq.empty,
                         pages: PageGenConfig = PageGenConfig())

object Replace {

  val parser = new scopt.OptionParser[ReplaceConfig]("scalawiki") {
    head("scalawiki", "0.5")

    opt[Seq[String]]('c', "cat")
      .action((cat, c) => c.copy(pages = c.pages.copy(cat = c.pages.cat ++ cat)))
      .text("Work on all pages which are in a specific category.")
      .unbounded()

    Seq(
      opt[Seq[String]]("ns"),
      opt[Seq[String]]("namespace"),
      opt[Seq[String]]("namespaces")
    ).map(_.action((ns, c) => c.copy(pages = c.pages.copy(namespaces = c.pages.namespaces ++ ns)))
      .text("Work on all pages in given namespaces .")
      .unbounded()
    )

    opt[Boolean]('r', "regex")
      .action((r, c) => c.copy(regex = r))
      .text("Make replacements using regular expressions.")

    arg[String]("replacements")
      .unbounded().optional().action((r, c) =>
      c.copy(replacements = c.replacements :+ r))
      .text("Replacement pairs.")

  }


  def main(args: Array[String]) {

    val conf = parser.parse(args, ReplaceConfig())

    conf.foreach { c =>
      if (c.replacements.length % 2 != 0) {
        println("Incomplete replacement pair.")
        return
      }
    }
  }
}

object TextLib {

  /**
    *
    * @param text text to process
    * @param old regex pattern string to replace
    * @param replacement replacement
    * @param exceptions text that matches these regex exceptions should not have replacements
    * @param ignoreCase match both lower- and uppercase characters for replacement
    * @param marker add this marker after the last replace or the end of text
    * @return text with replacements
    */
  def replaceExcept(text: String,
                    old: String,
                    replacement: String,
                    exceptions: Seq[Regex] = Seq.empty,
                    ignoreCase: Boolean = false,
                    marker: Option[String] = None) = {

    val regex = new ReplacementBase(old, replacement, None, true, ignoreCase).regex
    var markerPos = text.length
    val exceptionMatches = exceptions.map(ex => ex.findAllMatchIn(text).buffered)

    val it = RegexFactory.replacementIterator(text, regex)
    it foreach { m =>

      exceptionMatches.foreach(_.dropWhile(_.end < m.start))
      val inException = exceptionMatches.exists(_.head.start <= m.start)

      if (!inException) {
        markerPos = m.end
        it replace replacement
      }
    }

    val result = it.replaced

    marker.fold(result) { m =>
      result.substring(0, markerPos) + m + result.substring(markerPos)
    }
  }

}

