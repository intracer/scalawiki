package org.scalawiki.bots

import org.specs2.mutable.Specification

class PageGeneratorsSpec extends Specification {
  def parse(args: Seq[String]) = new PageGenerators(args) {
    verify()
  }

  "category" should {
    "be empty" in {
      parse(Nil).category.toOption === None
    }

    "be some" in {
      parse(Seq("--cat", "Category")).category() === "Category"
//      parse(Seq("-cat:Category")).values("category") === Seq("Category")
    }

//    "be two" in {
//      val cats = Seq("Cat1", "Cat2")
//      parse(Seq("-cat", "Cat1,Cat2")).values("category") === cats
//      parse(Seq("-cat", "Cat1|Cat2")).values("category") === cats
//
//
////      parse(Seq("-cat", "Cat1", "Cat2")).values("category") === cats
//    }
  }

  // TODO process namespaces
  "namespace" should {
    "be empty" in {
      parse(Nil).namespace.toOption === None
    }

    "be some text" in {
      parse(Seq("--namespace", "Help")).namespace() === "Help"
    }

    "be some number" in {
      parse(Seq("--namespace", "6")).namespace() === "6"
    }

//    "be two" in {
//      val ns = Seq("14", "8")
//      parse(Seq("-ns", "14,8")).values("namespace") === ns
//      parse(Seq("-namespace", "14,8")).values("namespace") === ns
//      parse(Seq("-namespaces", "14,8")).values("namespace") === ns
//    }
  }
}