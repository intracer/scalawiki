package org.scalawiki.bots

import org.scalawiki.dto.Page
import org.specs2.mutable.Specification

class PageFromFileBotSpec extends Specification {

  val titles = Seq("PageName", "AnotherPageName")

  val texts = Seq(
    "'''PageName'''\nText here\n",
    "'''AnotherPageName'''\nAnother text"
  )

  val textsWithoutTitles = Seq(
    "\nText here\n",
    "\nAnother text"
  )

  val docExample =
    """{{-start-}}
      |'''PageName'''
      |Text here
      |
      |{{-end-}}
      |{{-start-}}
      |'''AnotherPageName'''
      |Another text
      |{{-end-}}""".stripMargin.replaceAll("\r?\n", "\n")

  "pages" should {

    "support default start and end parameters" in {
      val pages = PageFromFileBot.pages(docExample).toBuffer
      pages.size === 2
      pages.map(_.title) === titles
      pages.flatMap(_.text) === texts
    }

    "support windows newlines" in {
      val pages =
        PageFromFileBot.pages(docExample.replaceAll("\n", "\r\n")).toBuffer
      pages.size === 2
      pages.map(_.title) === titles
      pages.flatMap(_.text) === texts.map(_.replaceAll("\n", "\r\n"))
    }

    "support start and end parameters" in {
      val docExampleOwnDelimiter =
        """xxxx
          |'''PageName'''
          |Text here
          |
          |yyyy
          |xxxx
          |'''AnotherPageName'''
          |Another text
          |yyyy""".stripMargin.replaceAll("\r?\n", "\n")

      val pages = PageFromFileBot
        .pages(
          docExampleOwnDelimiter,
          PageFromFileFormat(start = "xxxx", end = "yyyy")
        )
        .toBuffer
      pages.size === 2
      pages.map(_.title) === titles
      pages.flatMap(_.text) === texts
    }

    "support noTitle" in {
      val pages = PageFromFileBot.pages(docExample, noTitle = true).toBuffer
      pages.size === 2
      pages.map(_.title) === titles
      pages.flatMap(_.text) === textsWithoutTitles
    }

    "join title in page text" in {
      val pages = titles.zip(texts) map { case (title, text) =>
        Page(title).withText(text)
      }
      PageFromFileBot.join(pages, includeTitle = false) === docExample
    }

    "join include title" in {
      val pages = titles.zip(textsWithoutTitles) map { case (title, text) =>
        Page(title).withText(text)
      }
      PageFromFileBot.join(pages, includeTitle = true) === docExample
    }

    "process many pages in" in {
      def mb(bytes: Long): Double = bytes / Math.pow(2, 20)
      val runtime = Runtime.getRuntime

      val maxMemMB = mb(runtime.maxMemory)
      val words = 50
      val lines = 10
      val articles = Math.min(5000, 5000 * (maxMemMB / 400)).toInt
      println(
        s"$maxMemMB MB heap should be enough to process at least $articles articles"
      )

      new StringBuilder().append()
      val text = Array
        .fill(lines) {
          Array.fill(words)("wikitext").mkString(" ")
        }
        .mkString("{{-start-}}\n'''PageName''' ", "\n", "\n{{-end-}}")

      val chunk = Array.fill(articles)(text).mkString("\n")

      val pages = PageFromFileBot.pages(chunk)
      val size = pages.size

      val used = mb(runtime.totalMemory() - runtime.freeMemory()).toInt
      println(s"Used $used MB")

      size === articles
    }
  }
}
