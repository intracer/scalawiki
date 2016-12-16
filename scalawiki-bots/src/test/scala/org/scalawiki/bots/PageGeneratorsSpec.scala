package org.scalawiki.bots

import org.specs2.mutable.Specification

class PageGeneratorsSpec extends Specification {
  def parse(args: Seq[String]) = Replace.parser.parse(args, ReplaceConfig()).map(_.pages)

  "category" should {
    "be empty" in {
      parse(Seq.empty) === Some(PageGenConfig())
    }

    "be some" in {
      val cfg = Some(PageGenConfig(cat = Seq("Category")))
      parse(Seq("--cat", "Category")) === cfg
      parse(Seq("--cat:Category")) === cfg
      parse(Seq("-c", "Category")) === cfg
      parse(Seq("-c:Category")) === cfg
    }

    "be two" in {
      val cfg = Some(PageGenConfig(cat = Seq("Cat1", "Cat2")))
      parse(Seq("--cat", "Cat1,Cat2")) === cfg
      parse(Seq("--cat:Cat1,Cat2")) === cfg

      parse(Seq("-c", "Cat1,Cat2")) === cfg
      parse(Seq("-c:Cat1,Cat2")) === cfg

      parse(Seq("--cat", "Cat1", "--cat", "Cat2")) === cfg
      parse(Seq("-c", "Cat1", "-c", "Cat2")) === cfg
    }
  }
}