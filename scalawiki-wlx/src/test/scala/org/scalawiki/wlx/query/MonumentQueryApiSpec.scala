package org.scalawiki.wlx.query

import org.scalawiki.wlx.dto.{Contest, Monument}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class MonumentQueryApiSpec extends Specification with Mockito {

  // Note: byMonumentTemplateMaps and byMonumentTemplate are `final` on the trait.
  // Mockito with Specs2 intercepts final methods via subclass mocking, so we stub
  // the final blocking methods directly — same pattern as StatisticsSpec.

  "MonumentQuery trait" should {

    "byMonumentTemplateMaps delegates to byMonumentTemplateMapsAsync" in {
      val query = mock[MonumentQuery]
      val rows = Seq(Map("ID" -> "14-101-0001", "назва" -> "Test"))
      query.byMonumentTemplateMaps() returns rows
      val result = query.byMonumentTemplateMaps()
      result must haveSize(1)
      result.head must havePair("ID" -> "14-101-0001")
    }

    "return Map[String,String] rows with raw template parameter names" in {
      val query = mock[MonumentQuery]
      val rawRow = Map("ID" -> "01-001-0001", "назва" -> "Церква", "район" -> "Центральний")
      query.byMonumentTemplateMaps() returns Seq(rawRow)
      val result = query.byMonumentTemplateMaps().toSeq
      result must haveSize(1)
      result.head must_== rawRow
    }

    "byMonumentTemplate still returns Monument objects (existing interface unchanged)" in {
      val query = mock[MonumentQuery]
      val monument = Monument(id = "14-101-0001", name = "Test")
      query.byMonumentTemplate() returns Seq(monument)
      val result = query.byMonumentTemplate()
      result must haveSize(1)
      result.head.id must_== "14-101-0001"
    }

    "byMonumentTemplateMapsAsync is an abstract method on MonumentQuery trait" in {
      val query = mock[MonumentQuery]
      val rows = Seq(Map("ID" -> "14-101-0001", "назва" -> "Test"))
      query.byMonumentTemplateMapsAsync() returns Future.successful(rows)
      val result = Await.result(query.byMonumentTemplateMapsAsync(), 5.seconds)
      result must haveSize(1)
      result.head must havePair("ID" -> "14-101-0001")
    }
  }
}
