package org.scalawiki.json

import org.scalawiki.dto.Page
import org.scalawiki.json.MwReads._
import org.specs2.matcher.MatchResult
import org.specs2.mutable._
import play.api.libs.json.Json

class PlayJsonSpec extends Specification {

  val pageStr = """{"pageid": 123, "ns": 4, "title": "PageTitle" }"""

  val queryType = "embeddedin"
  val queryContinue = "eicontinue"

  val queryStr = s""""query": {"$queryType": [$pageStr] }"""

  "Page" should {
    "parse successfully" in {
      val json = Json.parse(pageStr)
      val pageOpt = json.validate[Page].asOpt

      checkPageOpt(pageOpt)
    }
  }

  "One page query" should {
    val limitsStr = s"""{"limits": {"embeddedin": 500}, $queryStr}"""
    val json = Json.parse(limitsStr)

    "contain page" in {
      val pagesOpt = json.validate[Seq[Page]](pagesReads(queryType)).asOpt
      checkPages(pagesOpt)
    }

    "not have continue" in {
      val qc = json.validate[String](queryContinueReads(queryType, queryContinue)).asOpt
      qc must beNone
    }
  }

  "Legacy Multipage query" should {
    val queryContinueStr = s"""{"query-continue": {"$queryType": {"$queryContinue": "qcValue" }}, $queryStr}"""
    val json = Json.parse(queryContinueStr)

    "contain page" in {
      val pagesOpt = json.validate[Seq[Page]](pagesReads(queryType)).asOpt
      checkPages(pagesOpt)
    }

    "have continue" in {
      val qc = json.validate[String](queryContinueReads(queryType, queryContinue)).asOpt
      qc must beSome("qcValue")
    }
  }

  "New Multipage query" should {
    val queryContinueStr = s"""{$queryStr, "continue":{"continue":"-||","eicontinue":"qcValue"}}"""
    val json = Json.parse(queryContinueStr)

    "contain page" in {
      val pagesOpt = json.validate[Seq[Page]](pagesReads(queryType)).asOpt
      checkPages(pagesOpt)
    }

    "have continue" in {
      val qc = json.validate(continueReads(queryContinue)).asOpt.get
      qc.continue must beSome("-||")
      qc.prefixed must beSome("qcValue")
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
    page.id === 123
    page.ns === 4
    page.title === "PageTitle"
  }
}
