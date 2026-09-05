package org.scalawiki.wlx

import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.dto.lists.EmptyListConfig
import org.scalawiki.wlx.query.MonumentQuery.MonumentListPage
import org.specs2.mutable.Specification

import java.nio.file.Files
import java.time.ZonedDateTime

class MonumentDbCacheSpec extends Specification {

  private val listConfig = EmptyListConfig

  private def tmpCsv() =
    Files.createTempFile("monuments-", ".csv").toString

  private val pageA = MonumentListPage(
    title = "Вікіпедія:ВЛП/Списки/Львівська область",
    revId = Some(42L),
    timestamp = Some(ZonedDateTime.parse("2026-08-01T12:00:00Z")),
    monuments = Seq(
      Monument(
        page = "Вікіпедія:ВЛП/Списки/Львівська область",
        id = "46-101-0001",
        name = "Town hall, [[Lviv]]",
        year = Some("1835"),
        city = Some("Львів"),
        lat = Some("49.84"),
        lon = Some("24.03"),
        typ = Some("complex"),
        contest = Some(46L),
        otherParams = Map("власність" -> "державна", "нагляд" -> "так, з комами")
      ),
      Monument(
        page = "Вікіпедія:ВЛП/Списки/Львівська область",
        id = "46-101-0002",
        name = "Simple monument"
      )
    )
  )

  private val pageB = MonumentListPage(
    title = "Вікіпедія:ВЛП/Списки/Київ",
    revId = None,
    timestamp = None,
    monuments = Seq(
      Monument(page = "Вікіпедія:ВЛП/Списки/Київ", id = "80-000-0001", name = "Kyiv one")
    )
  )

  "MonumentDbCache" should {

    "round-trip pages, revisions and monuments" in {
      val path = tmpCsv()
      MonumentDbCache.write(path, Seq(pageA, pageB))

      val back = MonumentDbCache.read(path, listConfig)

      back.map(_.title) === Seq(pageA.title, pageB.title)
      back.map(_.revId) === Seq(Some(42L), None)
      back.map(_.timestamp) === Seq(pageA.timestamp, None)

      val a = back.head
      a.monuments.map(_.id) === Seq("46-101-0001", "46-101-0002")
      val m = a.monuments.head
      m.name === "Town hall, [[Lviv]]"
      m.year === Some("1835")
      m.lat === Some("49.84")
      m.contest === Some(46L)
      m.otherParams === Map("власність" -> "державна", "нагляд" -> "так, з комами")
      m.listConfig === Some(listConfig)

      back(1).monuments.map(_.id) === Seq("80-000-0001")
    }

    "return Nil for a missing file" in {
      MonumentDbCache.read("no-such-file-12345.csv", listConfig) === Nil
    }

    "write nothing but a header for pages with no monuments" in {
      val path = tmpCsv()
      MonumentDbCache.write(path, Seq(pageA.copy(monuments = Nil)))
      MonumentDbCache.read(path, listConfig) === Nil
    }
  }
}
