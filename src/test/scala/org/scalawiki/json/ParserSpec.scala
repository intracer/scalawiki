package org.scalawiki.json

import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.prop.{LangLinks, LlLimit, Prop}
import org.scalawiki.dto.cmd.query.{Query, TitlesParam}
import org.specs2.mutable.Specification

import scala.io.Source

class ParserSpec extends Specification {

  "parser" should {
    "parse page with lang links" in {
      val is = getClass.getResourceAsStream("/org/scalawiki/query/langLinks.json")
      is !== null
      val s = Source.fromInputStream(is).mkString

      val action = Action(Query(
        Prop(
          LangLinks(LlLimit("max"))
        ),
        TitlesParam(Seq("Article"))
      ))

      val parser = new Parser(action)

      val page = parser.parse(s).get.head

      val langLinks = page.langLinks
      langLinks.size === 6

      langLinks === Map(
        "de" -> "Artikel",
        "eo" -> "Artikolo",
        "fr" -> "Article",
        "it" -> "Articolo",
        "nl" -> "Artikel",
        "pt" -> "Artigo")
    }
  }

}
