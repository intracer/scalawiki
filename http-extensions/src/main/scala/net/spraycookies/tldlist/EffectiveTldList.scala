package net.spraycookies.tldlist

import scala.io.Source

trait EffectiveTldList {
  def contains(domain: String): Boolean
}

trait TrieTldList extends EffectiveTldList {
  def domainTrie: TldTrie

  private def contains(domain: List[String]) = domainTrie.contains(domain)
  def contains(domain: String) =
    domainTrie.contains(domain.split('.').reverse.toList)

}

object DefaultEffectiveTldList extends TrieTldList {
  private val lines = {
    val inputStream = getClass.getResourceAsStream("/effectivetlds.lst")
    Source
      .fromInputStream(inputStream, "UTF-8")
      .getLines
      .filterNot(_.startsWith("//"))
      .filterNot(_.isEmpty)
  }

  val domainTrie = TldTrie(lines)

}
