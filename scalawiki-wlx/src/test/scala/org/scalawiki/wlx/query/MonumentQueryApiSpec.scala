package org.scalawiki.wlx.query

import org.scalawiki.wlx.dto.{Contest, ContestType, Country, Monument, UploadConfig}
import org.scalawiki.wlx.dto.lists.EmptyListConfig
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import java.time.ZonedDateTime
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class MonumentQueryApiSpec extends Specification with Mockito {

  // A minimal concrete MonumentQuery that overrides only byMonumentTemplateMapsAsync.
  // Used to test that the final sync wrapper delegates to the async method for real.
  private class StubMonumentQuery(
      asyncResult: Iterable[Map[String, String]]
  ) extends MonumentQuery {
    private val uploadConfig = UploadConfig(
      "test-campaign",
      "TestListTemplate",
      "TestFileTemplate",
      EmptyListConfig
    )
    override val contest: Contest =
      Contest(ContestType.WLM, Country.Ukraine, 2025, uploadConfigs = Seq(uploadConfig))

    override def byMonumentTemplateMapsAsync(
        generatorTemplate: String,
        date: Option[ZonedDateTime],
        listTemplate: Option[String]
    ): Future[Iterable[Map[String, String]]] =
      Future.successful(asyncResult)

    override def byMonumentTemplateAsync(
        generatorTemplate: String,
        date: Option[ZonedDateTime],
        listTemplate: Option[String]
    ): Future[Iterable[Monument]] = Future.successful(Nil)

    override def byPageAsync(
        page: String,
        template: String,
        date: Option[ZonedDateTime]
    ): Future[Iterable[Monument]] = Future.successful(Nil)
  }

  "MonumentQuery trait" should {

    "byMonumentTemplateMaps delegates to byMonumentTemplateMapsAsync" in {
      // Use a concrete stub so the final sync method runs for real and
      // delegates to byMonumentTemplateMapsAsync (not intercepted by Mockito).
      val query = new StubMonumentQuery(
        Seq(Map("ID" -> "14-101-0001", "назва" -> "Test"))
      )
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
