package org.scalawiki.json

import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.list.{EmbeddedIn, ListParam}
import org.scalawiki.dto.cmd.query.prop.{LangLinks, LlLimit, Prop}
import org.scalawiki.dto.cmd.query.{Query, TitlesParam}
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import scala.io.Source

class ParserSpec extends Specification {

  val pageStr = """{"pageid": 123, "ns": 4, "title": "PageTitle" }"""

  val queryType = "embeddedin"
  val queryContinue = "eicontinue"

  val queryStr = s""""query": {"$queryType": [$pageStr] }"""

  val emptyAction = Action(Query(ListParam(EmbeddedIn())))

  "One page query" should {
    val limitsStr = s"""{"limits": {"embeddedin": 500}, $queryStr}"""

    val parser = new Parser(emptyAction)
    val pagesOpt = parser.parse(limitsStr).toOption

    "contain page" in {
      checkPages(pagesOpt)
    }

    "not have continue" in {
      parser.continue === Map.empty
    }
  }

  "New Multipage query" should {
    val queryContinueStr = s"""{$queryStr, "continue": {"continue": "-||", "eicontinue": "qcValue"}}"""
    val parser = new Parser(emptyAction)
    val pagesOpt = parser.parse(queryContinueStr).toOption

    "contain page" in {
      checkPages(pagesOpt)
    }

    "have continue" in {
      parser.continue === Map("continue" -> "-||", "eicontinue" -> "qcValue")
    }
  }

  def checkPages(pagesOpt: Option[Seq[Page]]): MatchResult[Any] = {
    pagesOpt.map { pages =>
      pages must have size 1
      checkPage(pages(0))
    }
      .getOrElse(pagesOpt must beSome)
  }

  def checkPageOpt(pageOpt: Option[Page]): MatchResult[Any] = {
    pageOpt.map(checkPage).getOrElse(pageOpt must beSome)
  }

  def checkPage(page: Page): MatchResult[Any] = {
    page.id === Some(123)
    page.ns === 4
    page.title === "PageTitle"
  }


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
