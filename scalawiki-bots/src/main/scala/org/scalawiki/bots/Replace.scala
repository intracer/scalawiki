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
    titlesR.exists(_.findFirstIn(text).isDefined)

}

object TextLib {

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

