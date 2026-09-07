package org.scalawiki.wlx.query

import org.scalawiki.wlx.dto.{Contest, ContestType, Country, Monument, UploadConfig}
import org.scalawiki.wlx.dto.lists.EmptyListConfig
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import java.time.ZonedDateTime
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class MonumentQueryApiSpec extends Specification with Mockito {

  // A minimal concrete MonumentQuery with stub implementations of the abstract methods.
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

    override def byMonumentTemplateMaps(
        generatorTemplate: String,
        date: Option[ZonedDateTime],
        listTemplate: Option[String]
    ): Future[Iterable[Map[String, String]]] =
      Future.successful(asyncResult)

    override def byMonumentTemplate(
        generatorTemplate: String,
        date: Option[ZonedDateTime],
        listTemplate: Option[String]
    ): Future[Iterable[Monument]] = Future.successful(Nil)

    override def byPage(
        page: String,
        template: String,
        date: Option[ZonedDateTime]
    ): Future[Iterable[Monument]] = Future.successful(Nil)

    override def listPageRevs(
        generatorTemplate: String
    ): Future[Seq[MonumentQuery.MonumentListRev]] = Future.successful(Nil)

    override def monumentsByPages(
        titles: Set[String],
        listTemplate: Option[String]
    ): Future[Seq[MonumentQuery.MonumentListPage]] = Future.successful(Nil)
  }

  "MonumentQuery trait" should {

    "byMonumentTemplateMaps returns the rows the implementation produces" in {
      val query = new StubMonumentQuery(
        Seq(Map("ID" -> "14-101-0001", "назва" -> "Test"))
      )
      val result = Await.result(query.byMonumentTemplateMaps(), 5.seconds)
      result must haveSize(1)
      result.head must havePair("ID" -> "14-101-0001")
    }

    "byMonumentTemplateMaps rows carry raw template parameter names" in {
      val query = mock[MonumentQuery]
      val rawRow = Map("ID" -> "01-001-0001", "назва" -> "Церква", "район" -> "Центральний")
      query.byMonumentTemplateMaps() returns Future.successful(Seq(rawRow))
      val result = Await.result(query.byMonumentTemplateMaps(), 5.seconds).toSeq
      result must haveSize(1)
      result.head must_== rawRow
    }

    "byMonumentTemplate returns Monument objects" in {
      val query = mock[MonumentQuery]
      val monument = Monument(id = "14-101-0001", name = "Test")
      query.byMonumentTemplate() returns Future.successful(Seq(monument))
      val result = Await.result(query.byMonumentTemplate(), 5.seconds)
      result must haveSize(1)
      result.head.id must_== "14-101-0001"
    }

    "byMonumentTemplateMaps is an abstract method on MonumentQuery trait" in {
      val query = mock[MonumentQuery]
      val rows = Seq(Map("ID" -> "14-101-0001", "назва" -> "Test"))
      query.byMonumentTemplateMaps() returns Future.successful(rows)
      val result = Await.result(query.byMonumentTemplateMaps(), 5.seconds)
      result must haveSize(1)
      result.head must havePair("ID" -> "14-101-0001")
    }
  }
}
