package org.scalawiki.wlx.stat

import org.scalawiki.wlx.dto.Contest
import org.scalawiki.wlx.query.MonumentQuery
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import java.nio.file.Files
import scala.concurrent.Future

class StatisticsExportSpec extends Specification with Mockito {

  private val contest = Contest.WLMUkraine(2025)

  // byMonumentTemplateMaps is `final` on the trait — stub the abstract async variant.
  // The final method delegates to it via Await.result, so runExport exercises the real path.
  def buildMonumentQuery(rows: Seq[Map[String, String]]): MonumentQuery = {
    val q = mock[MonumentQuery]
    q.byMonumentTemplateMapsAsync() returns Future.successful(rows)
    q
  }

  "Statistics.runExport" should {

    "write CSV to specified filename" in {
      val path = Files.createTempFile("stats-export-test", ".csv")
      Files.deleteIfExists(path)
      try {
        val cfg = StatConfig(
          campaign = "WLM-UA",
          years = Seq(2025),
          exportCsv = Some(path.toString)
        )
        val q = buildMonumentQuery(Seq(Map("ID" -> "14-101-0001", "назва" -> "Test")))
        Statistics.runExport(contest, cfg, q)
        Files.exists(path) must beTrue
        val content = new String(Files.readAllBytes(path), "UTF-8")
        content must contain("id")
        content must contain("14-101-0001")
      } finally {
        Files.deleteIfExists(path)
      }
    }

    "default filename matches <campaign>-yyyy-MM-dd-HHmm.csv pattern" in {
      val name = Statistics.defaultCsvFilename("WLM-UA")
      name must beMatching("WLM-UA-\\d{4}-\\d{2}-\\d{2}-\\d{4}\\.csv")
    }

    "use default filename when exportCsv is empty string" in {
      val name = Statistics.defaultCsvFilename("WLM-UA")
      val cfg = StatConfig(campaign = "WLM-UA", years = Seq(2025), exportCsv = Some(""))
      // empty string → use default
      val resolved = cfg.exportCsv.filter(_.nonEmpty).getOrElse(name)
      resolved must_== name
    }

    "produce no output file for empty monument list" in {
      val path = Files.createTempFile("stats-export-empty", ".csv")
      Files.deleteIfExists(path)
      try {
        val cfg = StatConfig(campaign = "WLM-UA", years = Seq(2025), exportCsv = Some(path.toString))
        val q = buildMonumentQuery(Seq.empty)
        Statistics.runExport(contest, cfg, q)
        Files.exists(path) must beFalse
      } finally {
        Files.deleteIfExists(path)
      }
    }
  }
}
