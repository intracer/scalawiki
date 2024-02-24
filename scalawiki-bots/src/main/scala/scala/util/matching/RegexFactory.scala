package scala.util.matching

import java.util.regex.Pattern

object RegexFactory {
  def regex(p: Pattern): Regex = new Regex(p)

  def replacementIterator(text: String, old: Regex) =
    new Regex.MatchIterator(text, old, Seq.empty).replacementData
}
