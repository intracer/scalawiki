package org.scalawiki.bots

import org.specs2.mutable.Specification

class PageFromFileBotSpec extends Specification {

  val docExampleDefaultParams =
    """{{-start-}}
      |'''PageName'''
      |Text here
      |
      |{{-end-}}
      |{{-start-}}
      |'''AnotherPageName'''
      |Another text
      |{{-end-}}""".stripMargin

  val docExample =
    """xxxx
      |'''PageName'''
      |Text here
      |
      |yyyy
      |xxxx
      |'''AnotherPageName'''
      |Another text
      |yyyy""".stripMargin

  "page from file" should {

    "support default start and end parameters" in {
      val pages = PageFromFileBot.pages(docExampleDefaultParams).toBuffer
      pages.size === 2
      pages.map(_.title) === Seq("PageName", "AnotherPageName")
      pages.flatMap(_.text) === Seq(
        "'''PageName'''\nText here\n",
        "'''AnotherPageName'''\nAnother text"
      )
    }

    "support start and end parameters" in {
      val pages = PageFromFileBot.pages(docExample, start = "xxxx", end = "yyyy").toBuffer
      pages.size === 2
      pages.map(_.title) === Seq("PageName", "AnotherPageName")
      pages.flatMap(_.text) === Seq(
        "'''PageName'''\nText here\n",
        "'''AnotherPageName'''\nAnother text"
      )
    }
  }
}
