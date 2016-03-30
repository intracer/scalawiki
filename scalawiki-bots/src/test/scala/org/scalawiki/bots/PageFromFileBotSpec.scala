package org.scalawiki.bots

import org.specs2.mutable.Specification

class PageFromFileBotSpec extends Specification {

  val docExample =
    """{{-start-}}
      |'''PageName'''
      |Text here
      |
      |{{-end-}}
      |{{-start-}}
      |'''AnotherPageName'''
      |Another text
      |{{-end-}}""".stripMargin

  val docExampleOwnDelimiter =
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
      val pages = PageFromFileBot.pages(docExample).toBuffer
      pages.size === 2
      pages.map(_.title) === Seq("PageName", "AnotherPageName")
      pages.flatMap(_.text) === Seq(
        "'''PageName'''\nText here\n",
        "'''AnotherPageName'''\nAnother text"
      )
    }

    "support start and end parameters" in {
      val pages = PageFromFileBot.pages(docExampleOwnDelimiter, start = "xxxx", end = "yyyy").toBuffer
      pages.size === 2
      pages.map(_.title) === Seq("PageName", "AnotherPageName")
      pages.flatMap(_.text) === Seq(
        "'''PageName'''\nText here\n",
        "'''AnotherPageName'''\nAnother text"
      )
    }

    "process many pages in" in {
      def mb(bytes: Long): Double = bytes / Math.pow(2, 20)
      val runtime = Runtime.getRuntime

      val maxMemMB = mb(runtime.maxMemory)
      val words = 50
      val lines = 10
      val articles = Math.min(5000, 5000 * (maxMemMB / 200)).toInt
      println(s"$maxMemMB MB heap should be enough to process at least $articles articles")

      new StringBuilder().append()
      val text = Array.fill(lines) {
        Array.fill(words)("wikitext").mkString(" ")
      }.mkString("{{-start-}}\n'''PageName''' ", "\n", "\n{{-end-}}")

      val chunk = Array.fill(articles)(text).mkString("\n")

      val pages = PageFromFileBot.pages(chunk)
      val size = pages.size

      size === articles

      val used  = mb(runtime.totalMemory() - runtime.freeMemory()).toInt
      println(s"Used $used MB")
      used should be lessThan 200
    }
  }
}
