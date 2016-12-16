package org.scalawiki.bots

import com.concurrentthought.cla.Args
import org.specs2.mutable.Specification

class PageGeneratorsSpec extends Specification {
  def parse(args: Seq[String]) = Args("", PageGenerators.opts).parse(args)

  "category" should {
    "be empty" in {
      parse(Seq.empty).values.get("category") === None
    }

    "be some" in {
      parse(Seq("-cat", "Category")).values("category") === Seq("Category")
//      parse(Seq("-cat:Category")).values("category") === Seq("Category")
    }

    "be two" in {
      val cats = Seq("Cat1", "Cat2")
      parse(Seq("-cat", "Cat1,Cat2")).values("category") === cats
      parse(Seq("-cat", "Cat1|Cat2")).values("category") === cats


//      parse(Seq("-cat", "Cat1", "Cat2")).values("category") === cats
    }
  }

  // TODO process namespaces
  "namespace" should {
    "be empty" in {
      parse(Seq.empty).values.get("ns") === None
    }

    "be some text" in {
      parse(Seq("-ns", "Help")).values("namespace") === Seq("Help")
      parse(Seq("-namespace", "Help")).values("namespace") === Seq("Help")
      parse(Seq("-namespaces", "Help")).values("namespace") === Seq("Help")
    }

    "be some number" in {
      parse(Seq("-ns", "6")).values("namespace") === Seq("6")
      parse(Seq("-namespace", "6")).values("namespace") === Seq("6")
      parse(Seq("-namespaces", "6")).values("namespace") === Seq("6")
    }

    "be two" in {
      val ns = Seq("14", "8")
      parse(Seq("-ns", "14,8")).values("namespace") === ns
      parse(Seq("-namespace", "14,8")).values("namespace") === ns
      parse(Seq("-namespaces", "14,8")).values("namespace") === ns
    }
  }
}