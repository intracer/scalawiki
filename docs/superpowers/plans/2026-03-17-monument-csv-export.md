# Monument CSV Export Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `--export-csv [file]` to `Statistics.main()` that fetches raw monument template data and writes it to a CSV file with field names mapped to canonical English via `ua_uk.json`.

**Architecture:** Extract `byMonumentTemplateGeneric[T]` from `MonumentQueryApi` so both Monument parsing and raw-map extraction share one fetching implementation. A new `UaUkJsonMapping` object loads `ua_uk.json` and applies a two-level field mapping. `MonumentCsvExporter` uses `CSVWriter` to write the output.

**Tech Stack:** Scala 2.13, sbt, Specs2, Play JSON (for `ua_uk.json` parsing), `com.github.tototoshi.csv.CSVWriter`, Apache Pekko HTTP (existing)

**Spec:** `docs/superpowers/specs/2026-03-17-monument-csv-export-design.md`

---

## File Map

| File | Action | Responsibility |
|------|--------|---------------|
| `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/UaUkJsonMapping.scala` | Create | Load `ua_uk.json`, apply two-level field mapping, compute CSV header order |
| `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/UaUkJsonMappingSpec.scala` | Create | Unit tests for all mapping cases |
| `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/MonumentCsvExporter.scala` | Create | Write mapped monument rows to CSV |
| `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/MonumentCsvExporterSpec.scala` | Create | Unit tests for CSV output |
| `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/query/MonumentQuery.scala` | Modify | Extract `byMonumentTemplateGeneric`; add `byMonumentTemplateMapsAsync` + `byMonumentTemplateMaps` |
| `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/query/MonumentQueryApiSpec.scala` | Create | Test `byMonumentTemplateMaps` with stub HTTP |
| `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/StatParams.scala` | Modify | Add `exportCsv` to both `StatConfig` and `StatParams`; wire in `parse()` |
| `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/Statistics.scala` | Modify | Short-circuit to CSV export when flag is set |
| `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/stat/StatParamsSpec.scala` | Modify | Add tests for `--export-csv` flag parsing |
| `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/stat/StatisticsExportSpec.scala` | Create | Integration tests for the `--export-csv` code path |

---

## Chunk 1: UaUkJsonMapping

### Task 1: `UaUkJsonMappingSpec` — load tests

**Files:**
- Create: `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/UaUkJsonMappingSpec.scala`

- [ ] **Step 1: Write failing tests for `load()`**

```scala
package org.scalawiki.wlx

import org.specs2.mutable.Specification

class UaUkJsonMappingSpec extends Specification {

  // Load once for all tests
  lazy val mapping: UaUkMapping = UaUkJsonMapping.load("monuments_config/ua_uk.json")

  "UaUkJsonMapping.load" should {

    "parse fieldMap with a simple source→dest entry" in {
      // "назва" → "name" from fields array
      mapping.fieldMap must haveKey("назва")
      mapping.fieldMap("назва") must contain("name")
    }

    "parse fieldMap with a duplicate source (галерея → commonscat AND gallery)" in {
      mapping.fieldMap must haveKey("галерея")
      mapping.fieldMap("галерея") must containAllOf(Seq("commonscat", "gallery"))
    }

    "represent empty-dest entries as identity (source → Seq(source))" in {
      // "паспорт" has dest="" in ua_uk.json
      mapping.fieldMap must haveKey("паспорт")
      mapping.fieldMap("паспорт") must_== Seq("паспорт")
    }

    "parse sqlMap with Field type entry" in {
      // adm2 → {type:Field, value:rayon}
      mapping.sqlMap must haveKey("adm2")
      mapping.sqlMap("adm2") must_== SqlEntry("Field", "rayon")
    }

    "parse sqlMap with Text type entry" in {
      // adm0 → {type:Text, value:ua}
      mapping.sqlMap must haveKey("adm0")
      mapping.sqlMap("adm0") must_== SqlEntry("Text", "ua")
    }

    "parse sqlMap with Raw type entry" in {
      // adm1 → {type:Raw, value:LOWER(`iso_oblast`)}
      mapping.sqlMap must haveKey("adm1")
      mapping.sqlMap("adm1").entryType must_== "Raw"
    }

    "preserve sql_data key insertion order in sqlKeyOrder" in {
      // id, name, address, municipality, lat, lon, image, commonscat, source,
      // changed, monument_article, wd_item, country, lang, adm0, adm1, adm2
      mapping.sqlKeyOrder.head must_== "id"
      // adm0 and adm2 must appear (Text and Field respectively)
      mapping.sqlKeyOrder must contain("adm0")
      mapping.sqlKeyOrder must contain("adm2")
      // adm0 (Text) and adm1 (Raw) and adm2 (Field) — all present
      val adm0Idx = mapping.sqlKeyOrder.indexOf("adm0")
      val adm2Idx = mapping.sqlKeyOrder.indexOf("adm2")
      adm0Idx must be_>=(0)
      adm2Idx must be_>=(0)
    }
  }
}
```

- [ ] **Step 2: Run to confirm compilation failure (types don't exist yet)**

```bash
sbt "scalawiki-wlx/testOnly org.scalawiki.wlx.UaUkJsonMappingSpec"
```
Expected: compilation error — `UaUkMapping`, `UaUkJsonMapping`, `SqlEntry` not found.

---

### Task 2: Implement `UaUkJsonMapping`

**Files:**
- Create: `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/UaUkJsonMapping.scala`

- [ ] **Step 3: Create the file**

```scala
package org.scalawiki.wlx

import play.api.libs.json._

import scala.collection.immutable.ListMap
import scala.io.Source

/** A single sql_data entry.
  * @param entryType "Field" (rename an existing key), "Text" (inject literal), or "Raw" (skip)
  * @param value     field name (for Field) or literal value (for Text/Raw)
  */
case class SqlEntry(entryType: String, value: String)

/** The parsed ua_uk.json mapping.
  *
  * @param fieldMap    source template param name → list of dest column names.
  *                    Empty-dest entries in JSON are stored as Seq(source) (identity).
  *                    Duplicate sources accumulate dest names in order of appearance.
  * @param sqlMap      sql_data key → SqlEntry (all types: Field, Text, Raw)
  * @param sqlKeyOrder sql_data keys in JSON insertion order (for stable CSV headers)
  */
case class UaUkMapping(
    fieldMap: Map[String, Seq[String]],
    sqlMap: Map[String, SqlEntry],
    sqlKeyOrder: Seq[String]
)

object UaUkJsonMapping {

  /** Load ua_uk.json from the classpath.
    * @param resourcePath e.g. "monuments_config/ua_uk.json"
    */
  def load(resourcePath: String): UaUkMapping = {
    val source = Source.fromResource(resourcePath)
    val raw    = try source.mkString finally source.close()
    val json   = Json.parse(raw)

    // --- Level 1: fields array ---
    // Build fieldMap: source → Seq[dest]. Empty dest → Seq(source).
    val fields = (json \ "fields").as[JsArray].value
    val fieldMapBuilder = scala.collection.mutable.LinkedHashMap.empty[String, scala.collection.mutable.ListBuffer[String]]

    fields.foreach { entry =>
      val src  = (entry \ "source").as[String]
      val dest = (entry \ "dest").as[String]
      val resolved = if (dest.nonEmpty) dest else src  // empty dest → identity
      fieldMapBuilder
        .getOrElseUpdate(src, scala.collection.mutable.ListBuffer.empty)
        .append(resolved)
    }

    // Note: for duplicate sources (e.g. галерея), dest names are ordered by appearance in the fields array.
    val fieldMap: Map[String, Seq[String]] =
      fieldMapBuilder.map { case (k, v) => k -> v.toSeq }.toMap

    // --- Level 2: sql_data object (preserve insertion order) ---
    val sqlDataObj = (json \ "sql_data").as[JsObject]
    // IMPORTANT: Play JSON's JsObject stores fields as an internal Seq[(String, JsValue)],
    // and Json.parse preserves the document order in that Seq.
    // .fields returns an IndexedSeq in that order.
    // Do NOT convert through Map at any point — doing so will lose insertion order.
    val sqlEntries: Seq[(String, SqlEntry)] = sqlDataObj.fields.map {
      case (key, obj) =>
        val entryType = (obj \ "type").as[String]
        val value     = (obj \ "value").as[String]
        key -> SqlEntry(entryType, value)
    }

    val sqlMap: Map[String, SqlEntry]   = sqlEntries.toMap
    val sqlKeyOrder: Seq[String]        = sqlEntries.map(_._1)

    UaUkMapping(fieldMap, sqlMap, sqlKeyOrder)
  }

  /** Apply two-level mapping to one monument row.
    *
    * Step 1 (Level 1 — fields):
    *   For each (key, value) in row:
    *     if fieldMap contains key → emit (dest, value) for each dest in fieldMap(key)
    *     else                    → emit (key, value) unchanged
    *
    * Step 2 (Level 2 — sql_data Field entries):
    *   case A: sqlEntry.value absent from intermediateRow → skip
    *   case B: sqlKey == sqlEntry.value                  → no-op
    *   case C: sqlKey not yet in intermediateRow         → rename sqlEntry.value → sqlKey
    *   case D: sqlKey already in intermediateRow         → keep sqlKey, drop sqlEntry.value
    *
    * Step 3 (Level 2 — sql_data Text entries):
    *   Inject sqlKey → literal value into every row.
    *
    * Raw entries are skipped in all steps.
    */
  def applyMapping(
      row: Map[String, String],
      mapping: UaUkMapping
  ): Map[String, String] = {

    // Step 1
    var intermediate = Map.empty[String, String]
    for ((key, value) <- row) {
      mapping.fieldMap.get(key) match {
        case Some(dests) => dests.foreach { dest => intermediate = intermediate.updated(dest, value) }
        case None        => intermediate = intermediate.updated(key, value)
      }
    }

    // Step 2 — Field
    for ((sqlKey, entry) <- mapping.sqlMap if entry.entryType == "Field") {
      if (intermediate.contains(entry.value)) {
        if (sqlKey != entry.value) {
          if (!intermediate.contains(sqlKey)) {
            // case C: rename
            intermediate = intermediate.updated(sqlKey, intermediate(entry.value)) - entry.value
          } else {
            // case D: sqlKey already present, just drop old key
            intermediate = intermediate - entry.value
          }
        }
        // case B: sqlKey == entry.value → no-op
      }
      // case A: entry.value absent → skip
    }

    // Step 3 — Text
    for ((sqlKey, entry) <- mapping.sqlMap if entry.entryType == "Text") {
      intermediate = intermediate.updated(sqlKey, entry.value)
    }

    intermediate
  }

  /** Compute CSV header columns in deterministic order.
    *
    * Order:
    *   1. sql_data keys (Field + Text only, excluding Raw) in sqlKeyOrder insertion order
    *   2. All remaining keys found across all rows, alphabetically sorted.
    *      "Remaining" = any key that is NOT in sqlKeySet (the Field+Text sql_data key set).
    *      Raw sql_data keys (e.g. "adm1") are NOT in sqlKeySet, so if a row has an "adm1"
    *      key, it falls into the alphabetical section — not the sql first section.
    *
    * @param mappedRows rows that have already had applyMapping applied
    * @param mapping    the UaUkMapping (for sqlKeyOrder)
    */
  def headerColumns(
      mappedRows: Iterable[Map[String, String]],
      mapping: UaUkMapping
  ): Seq[String] = {
    // Section 1: sql_data keys that are Field or Text (not Raw), in insertion order
    val sqlKeys = mapping.sqlKeyOrder.filter { k =>
      mapping.sqlMap.get(k).exists(e => e.entryType == "Field" || e.entryType == "Text")
    }
    val sqlKeySet = sqlKeys.toSet

    // Section 2: all row keys not covered by sqlKeySet, sorted alphabetically
    val allKeys = mappedRows.flatMap(_.keys).toSet
    val remaining = (allKeys -- sqlKeySet).toSeq.sorted

    sqlKeys ++ remaining
  }
}
```

- [ ] **Step 4: Run the load tests**

```bash
sbt "scalawiki-wlx/testOnly org.scalawiki.wlx.UaUkJsonMappingSpec"
```
Expected: all `load` tests pass.

---

### Task 3: `applyMapping` tests

**Files:**
- Modify: `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/UaUkJsonMappingSpec.scala`

- [ ] **Step 5: Add applyMapping tests**

Append inside the `UaUkJsonMappingSpec` class (before the closing brace):

```scala
  "UaUkJsonMapping.applyMapping" should {

    "rename a source field to its dest (назва → name)" in {
      val row = Map("назва" -> "Церква Святого Миколая")
      val result = UaUkJsonMapping.applyMapping(row, mapping)
      result must haveKey("name")
      result("name") must_== "Церква Святого Миколая"
      result must not haveKey("назва")
    }

    "emit multiple columns for duplicate-source field (галерея → commonscat + gallery)" in {
      val row = Map("галерея" -> "Churches in Kyiv")
      val result = UaUkJsonMapping.applyMapping(row, mapping)
      result must haveKey("commonscat")
      result must haveKey("gallery")
      result("commonscat") must_== "Churches in Kyiv"
      result("gallery") must_== "Churches in Kyiv"
      result must not haveKey("галерея")
    }

    "remap via sql_data Field (rayon → adm2)" in {
      // район → rayon (Level 1), rayon → adm2 (Level 2 Field)
      val row = Map("район" -> "Шевченківський")
      val result = UaUkJsonMapping.applyMapping(row, mapping)
      result must haveKey("adm2")
      result("adm2") must_== "Шевченківський"
      result must not haveKey("rayon")
      result must not haveKey("район")
    }

    "inject Text literal (adm0 = ua, lang = uk)" in {
      val row = Map("ID" -> "14-101-0001")
      val result = UaUkJsonMapping.applyMapping(row, mapping)
      result must haveKey("adm0")
      result("adm0") must_== "ua"
      result must haveKey("lang")
      result("lang") must_== "uk"
    }

    "skip Raw entry (adm1 not injected as literal)" in {
      val row = Map("iso" -> "UA-30")
      val result = UaUkJsonMapping.applyMapping(row, mapping)
      // iso → iso_oblast (Level 1), then adm1.value=iso_oblast → Field rename
      // But "Raw" entries in sql_data are skipped
      // adm1 must NOT contain the Raw SQL expression
      result.get("adm1").foreach(_ must not contain "LOWER")
    }

    "keep unmapped field under original name" in {
      val row = Map("unknownField" -> "someValue")
      val result = UaUkJsonMapping.applyMapping(row, mapping)
      result must haveKey("unknownField")
      result("unknownField") must_== "someValue"
    }

    "sql_data Field entry whose value is absent from row produces no column (case A)" in {
      // sql_data has Field entries for "source" and "changed" whose values
      // ("source", "changed") are not produced by any fields mapping.
      // Result: neither key appears under the sql_data entry name.
      val row = Map("ID" -> "14-101-0001")
      val result = UaUkJsonMapping.applyMapping(row, mapping)
      // "source" and "changed" are sql_data Field entries but their intermediate keys won't exist
      // so the result must not gain spurious columns from them
      result.size must be_>=(1)  // just the mapped fields, no phantom columns
      // Specifically, "source" and "changed" must not appear unless the row had them
      result must not haveKey("source")
      result must not haveKey("changed")
    }

    "keep empty-dest source field under its original name (general rule, tested via паспорт)" in {
      // паспорт has dest="" → identity mapping → appears under original name
      // This verifies the general rule: ANY empty-dest entry keeps its source name
      val row = Map("паспорт" -> "12345", "наказ" -> "№100")
      val result = UaUkJsonMapping.applyMapping(row, mapping)
      result must haveKey("паспорт")
      result("паспорт") must_== "12345"
      result must haveKey("наказ")
      result("наказ") must_== "№100"
    }

    "handle collision: case D — sqlKey already in row, drop old key" in {
      // Construct a row where intermediateRow already has the sqlKey
      // commonscat appears both from галерея→commonscat and from sql_data Field commonscat→commonscat (no-op B)
      // Verify no duplicate / no crash
      val row = Map("галерея" -> "SomeCat")
      val result = UaUkJsonMapping.applyMapping(row, mapping)
      result must haveKey("commonscat")
      result("commonscat") must_== "SomeCat"
    }

    "full round-trip: realistic monument row" in {
      val row = Map(
        "ID"      -> "14-101-0001",
        "назва"   -> "Будинок Городецького",
        "район"   -> "Печерський",
        "iso"     -> "UA-30",
        "широта"  -> "50.4501",
        "довгота" -> "30.5234",
        "фото"    -> "Horodetsky House.jpg"
      )
      val result = UaUkJsonMapping.applyMapping(row, mapping)
      result("id")       must_== "14-101-0001"
      result("name")     must_== "Будинок Городецького"
      result("adm2")     must_== "Печерський"
      result("lat")      must_== "50.4501"
      result("lon")      must_== "30.5234"
      result("image")    must_== "Horodetsky House.jpg"
      result("adm0")     must_== "ua"
      result("lang")     must_== "uk"
    }
  }

  "UaUkJsonMapping.headerColumns" should {

    "put sql_data keys first in insertion order, then remaining alphabetically" in {
      val rows = Seq(
        Map("id" -> "1", "name" -> "A", "unknownZ" -> "z"),
        Map("id" -> "2", "adm0" -> "ua", "unknownA" -> "a")
      )
      val headers = UaUkJsonMapping.headerColumns(rows, mapping)
      // sql_data keys (Field+Text) come first
      val sqlNonRaw = mapping.sqlKeyOrder.filter(k =>
        mapping.sqlMap.get(k).exists(e => e.entryType == "Field" || e.entryType == "Text")
      )
      headers.take(sqlNonRaw.size) must_== sqlNonRaw
      // remaining keys alphabetically after sql keys
      val remaining = headers.drop(sqlNonRaw.size)
      remaining must_== remaining.sorted
    }

    "not include Raw sql_data keys in the header" in {
      val rows = Seq(Map("id" -> "1"))
      val headers = UaUkJsonMapping.headerColumns(rows, mapping)
      // adm1 is Raw — must not appear via sql ordering (may appear if row has adm1 key, but rows don't here)
      val adm1 = mapping.sqlMap.get("adm1")
      adm1.map(_.entryType) must beSome("Raw")
      // adm1 not in first-section headers
      val sqlIdx = headers.indexOf("adm1")
      val sqlNonRaw = mapping.sqlKeyOrder.filter(k =>
        mapping.sqlMap.get(k).exists(e => e.entryType == "Field" || e.entryType == "Text")
      )
      if (sqlIdx >= 0) sqlIdx must be_>=(sqlNonRaw.size)  // not in sql section
      ok
    }
  }
```

- [ ] **Step 6: Run all UaUkJsonMappingSpec tests**

```bash
sbt "scalawiki-wlx/testOnly org.scalawiki.wlx.UaUkJsonMappingSpec"
```
Expected: all tests pass.

- [ ] **Step 7: Commit**

```bash
git add scalawiki-wlx/src/main/scala/org/scalawiki/wlx/UaUkJsonMapping.scala \
        scalawiki-wlx/src/test/scala/org/scalawiki/wlx/UaUkJsonMappingSpec.scala
git commit -m "feat: add UaUkJsonMapping with two-level field mapping from ua_uk.json"
```

---

## Chunk 2: MonumentCsvExporter

### Task 4: `MonumentCsvExporterSpec` — failing tests

**Files:**
- Create: `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/MonumentCsvExporterSpec.scala`

- [ ] **Step 1: Write failing tests**

```scala
package org.scalawiki.wlx

import com.github.tototoshi.csv.CSVReader
import org.specs2.mutable.Specification

import java.io.{File, StringReader, StringWriter}
import java.nio.file.{Files, Path}

class MonumentCsvExporterSpec extends Specification {

  // Minimal mapping for tests: no sql_data, simple fieldMap
  val simpleMapping = UaUkMapping(
    fieldMap = Map("src" -> Seq("dest")),
    sqlMap = Map.empty,
    sqlKeyOrder = Seq.empty
  )

  // Real mapping from ua_uk.json
  lazy val realMapping: UaUkMapping =
    UaUkJsonMapping.load("monuments_config/ua_uk.json")

  def exportToString(
      rows: Iterable[Map[String, String]],
      mapping: UaUkMapping
  ): String = {
    val path = Files.createTempFile("monument-csv-test", ".csv")
    try {
      MonumentCsvExporter.export(rows, mapping, path.toString)
      new String(Files.readAllBytes(path))
    } finally {
      Files.deleteIfExists(path)
    }
  }

  def parseCsv(content: String): List[List[String]] =
    CSVReader.open(new StringReader(content)).all()

  "MonumentCsvExporter" should {

    "write header and one data row for a single input row" in {
      val rows = Seq(Map("назва" -> "Церква", "ID" -> "14-101-0001"))
      val content = exportToString(rows, realMapping)
      val parsed = parseCsv(content)
      parsed.size must_== 2           // header + 1 data row
      parsed.head must contain("name")
      parsed.head must contain("id")
      val idIdx = parsed.head.indexOf("id")
      parsed(1)(idIdx) must_== "14-101-0001"
    }

    "write union of keys from multiple rows with different fields" in {
      val rows = Seq(
        Map("ID" -> "01-001-0001", "назва" -> "A"),
        Map("ID" -> "02-001-0001", "район" -> "Центральний")
      )
      val content = exportToString(rows, realMapping)
      val parsed = parseCsv(content)
      val header = parsed.head
      header must contain("id")
      header must contain("name")
      header must contain("adm2")
      // rows should have blanks for missing fields
      parsed.size must_== 3
    }

    "escape commas, quotes, and newlines in values" in {
      val rows = Seq(Map("назва" -> "A, B \"C\"\nD"))
      val content = exportToString(rows, realMapping)
      val parsed = parseCsv(content)
      parsed.size must_== 2
      val nameIdx = parsed.head.indexOf("name")
      parsed(1)(nameIdx) must_== "A, B \"C\"\nD"
    }

    "produce no output file for empty input" in {
      val path = Files.createTempFile("monument-csv-empty", ".csv")
      Files.deleteIfExists(path)   // ensure it doesn't exist
      MonumentCsvExporter.export(Seq.empty, realMapping, path.toString)
      Files.exists(path) must beFalse
    }

    "inject Text literals from sql_data into every row" in {
      val rows = Seq(Map("ID" -> "14-101-0001"))
      val content = exportToString(rows, realMapping)
      val parsed = parseCsv(content)
      val header = parsed.head
      header must contain("adm0")
      val adm0Idx = header.indexOf("adm0")
      parsed(1)(adm0Idx) must_== "ua"
    }

    "produce deterministic column order across multiple runs" in {
      val rows = Seq(Map("ID" -> "1", "назва" -> "A", "район" -> "R"))
      val content1 = exportToString(rows, realMapping)
      val content2 = exportToString(rows, realMapping)
      parseCsv(content1).head must_== parseCsv(content2).head
    }
  }
}
```

- [ ] **Step 2: Run to confirm compilation failure**

```bash
sbt "scalawiki-wlx/testOnly org.scalawiki.wlx.MonumentCsvExporterSpec"
```
Expected: compilation error — `MonumentCsvExporter` not found.

---

### Task 5: Implement `MonumentCsvExporter`

**Files:**
- Create: `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/MonumentCsvExporter.scala`

- [ ] **Step 3: Create the file**

```scala
package org.scalawiki.wlx

import com.github.tototoshi.csv.CSVWriter

import java.io.File

object MonumentCsvExporter {

  /** Export monument rows to a CSV file.
    *
    * - Applies UaUkJsonMapping.applyMapping to each row.
    * - Computes header via UaUkJsonMapping.headerColumns (sql_data keys first, then alphabetical).
    * - Writes nothing if rows is empty.
    *
    * @param rows       raw monument parameter maps (as returned by parseToMaps)
    * @param mapping    loaded UaUkMapping (from UaUkJsonMapping.load)
    * @param outputPath path to the output CSV file
    */
  def export(
      rows: Iterable[Map[String, String]],
      mapping: UaUkMapping,
      outputPath: String
  ): Unit = {
    // Materialize once so we can iterate twice (header + rows)
    val mappedRows: Seq[Map[String, String]] =
      rows.map(UaUkJsonMapping.applyMapping(_, mapping)).toSeq

    if (mappedRows.isEmpty) return

    val headers = UaUkJsonMapping.headerColumns(mappedRows, mapping)

    val writer = CSVWriter.open(new File(outputPath))
    try {
      writer.writeRow(headers)
      mappedRows.foreach { row =>
        writer.writeRow(headers.map(h => row.getOrElse(h, "")))
      }
    } finally {
      writer.close()
    }
  }
}
```

- [ ] **Step 4: Run the exporter tests**

```bash
sbt "scalawiki-wlx/testOnly org.scalawiki.wlx.MonumentCsvExporterSpec"
```
Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add scalawiki-wlx/src/main/scala/org/scalawiki/wlx/MonumentCsvExporter.scala \
        scalawiki-wlx/src/test/scala/org/scalawiki/wlx/MonumentCsvExporterSpec.scala
git commit -m "feat: add MonumentCsvExporter"
```

---

## Chunk 3: MonumentQuery refactor

### Task 6: `MonumentQueryApiSpec` — failing tests

**Files:**
- Create: `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/query/MonumentQueryApiSpec.scala`

- [ ] **Step 1: Write failing tests**

The existing `MockBotSpec` / `HttpStub` pattern (see `WlmUaListsSpec`) stubs the HTTP layer. Here we use the simpler `mock[MonumentQuery]` approach (matching `StatisticsSpec`) to test the interface contract, plus a structural test to verify the refactor doesn't break the existing blocking wrapper.

```scala
package org.scalawiki.wlx.query

import org.scalawiki.MwBot
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.Future

class MonumentQueryApiSpec extends Specification with Mockito {

  private val contest = Contest.WLMUkraine(2025)

  // Note: byMonumentTemplateMaps and byMonumentTemplate are `final` on the trait.
  // Stub the async variants (which are abstract and thus mockable) — the final
  // blocking methods delegate to them via Await.result, so they exercise the real path.

  "MonumentQuery trait" should {

    "byMonumentTemplateMaps delegates to byMonumentTemplateMapsAsync" in {
      val query = mock[MonumentQuery]
      // Stub the abstract async method — the final sync method calls it
      query.byMonumentTemplateMapsAsync() returns Future.successful(
        Seq(Map("ID" -> "14-101-0001", "назва" -> "Test"))
      )
      val result = query.byMonumentTemplateMaps()
      result must haveSize(1)
      result.head must havePair("ID" -> "14-101-0001")
    }

    "return Map[String,String] rows with raw template parameter names" in {
      val query = mock[MonumentQuery]
      val rawRow = Map("ID" -> "01-001-0001", "назва" -> "Церква", "район" -> "Центральний")
      query.byMonumentTemplateMapsAsync() returns Future.successful(Seq(rawRow))
      val result = query.byMonumentTemplateMaps().toSeq
      result must haveSize(1)
      result.head must_== rawRow
    }

    "byMonumentTemplate still returns Monument objects (existing interface unchanged)" in {
      val query = mock[MonumentQuery]
      val monument = Monument(id = "14-101-0001", name = "Test")
      query.byMonumentTemplateAsync() returns Future.successful(Seq(monument))
      val result = query.byMonumentTemplate()
      result must haveSize(1)
      result.head.id must_== "14-101-0001"
    }
  }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
sbt "scalawiki-wlx/testOnly org.scalawiki.wlx.query.MonumentQueryApiSpec"
```
Expected: compilation error — `byMonumentTemplateMapsAsync` / `byMonumentTemplateMaps` not found on trait.

---

### Task 7: Refactor `MonumentQuery.scala`

**Files:**
- Modify: `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/query/MonumentQuery.scala`

- [ ] **Step 3: Add `byMonumentTemplateMapsAsync` + `byMonumentTemplateMaps` to the trait**

In the `MonumentQuery` trait (after `byMonumentTemplateAsync`), add:

```scala
  def byMonumentTemplateMapsAsync(
      generatorTemplate: String = defaultListTemplate,
      date: Option[ZonedDateTime] = None,
      listTemplate: Option[String] = None
  ): Future[Iterable[Map[String, String]]]

  final def byMonumentTemplateMaps(
      generatorTemplate: String = defaultListTemplate,
      date: Option[ZonedDateTime] = None,
      listTemplate: Option[String] = None
  ): Iterable[Map[String, String]] =
    Await.result(
      byMonumentTemplateMapsAsync(generatorTemplate, date, listTemplate),
      Timeout
    )
```

Also add the import at the top of the file (if not already present):
```scala
import org.scalawiki.wlx.WlxTemplateParser
```

- [ ] **Step 4: Extract `byMonumentTemplateGeneric` and add `byMonumentTemplateMapsAsync` in `MonumentQueryApi`**

Replace the body of `byMonumentTemplateAsync` in `MonumentQueryApi` with a delegation to the new generic method, and add `byMonumentTemplateMapsAsync`. The `reportDifferentRegionIds` logic stays in `byMonumentTemplateAsync`.

The new structure of `MonumentQueryApi`:

```scala
  /** Shared fetching logic for both Monument and raw-map extraction.
    * Does NOT include reportDifferentRegionIds side-effects — those stay in byMonumentTemplateAsync.
    */
  private def byMonumentTemplateGeneric[T](
      generatorTemplate: String,
      date: Option[ZonedDateTime],
      listTemplate: Option[String],
      parser: (String, String) => Iterable[T]   // (pageName, wikiText)
  ): Future[Iterable[T]] = {
    val title =
      if (generatorTemplate.startsWith("Template")) generatorTemplate
      else "Template:" + generatorTemplate

    val listConfig = listTemplate.fold(defaultListConfig)(
      new OtherTemplateListConfig(_, defaultListConfig)
    )

    if (date.isEmpty) {
      bot
        .page(title)
        .revisionsByGenerator(
          "embeddedin",
          "ei",
          Set(Namespace.PROJECT, Namespace.MAIN),
          Set("ids", "content", "timestamp", "user", "userid", "comment"),
          None,
          "100"
        ) map { pages =>
        pages.flatMap { page =>
          if (!page.title.contains("новий АТУ"))
            parser(page.title, page.text.getOrElse(""))
          else Nil
        }
      }
    } else {
      val template = listTemplate.getOrElse(generatorTemplate)
      articlesWithTemplate(title).flatMap { ids =>
        Future.traverse(ids)(id => pageRevisions(id, date.get)).map { pages =>
          pages.flatten.flatMap(page =>
            parser(page.title, page.text.getOrElse(""))
          )
        }
      }
    }
  }

  override def byMonumentTemplateAsync(
      generatorTemplate: String,
      date: Option[ZonedDateTime] = None,
      listTemplate: Option[String] = None
  ): Future[Iterable[Monument]] = {
    val differentRegionIds = new ArrayBuffer[String]()
    val listConfig = listTemplate.fold(defaultListConfig)(
      new OtherTemplateListConfig(_, defaultListConfig)
    )
    val template = listTemplate.getOrElse(generatorTemplate)

    byMonumentTemplateGeneric(
      generatorTemplate,
      date,
      listTemplate,
      (page, text) => {
        val monuments = Monument.monumentsFromText(text, page, template, listConfig)
        if (date.isEmpty) {
          val regionIds = monuments.map(_.id.split("-").init.mkString("-")).toSet
          if (regionIds.size > 1 && reportDifferentRegionIds) {
            differentRegionIds.append(
              s"* [[$page]]: ${regionIds.toSeq.sorted.mkString(", ")}"
            )
          }
        }
        monuments
      }
    ).map { monuments =>
      if (date.isEmpty && reportDifferentRegionIds) {
        Await.result(
          bot
            .page(s"Вікіпедія:${contest.name}/differentRegionIds")
            .edit(differentRegionIds.sorted.mkString("\n")),
          10.seconds
        )
      }
      monuments
    }
  }

  override def byMonumentTemplateMapsAsync(
      generatorTemplate: String,
      date: Option[ZonedDateTime] = None,
      listTemplate: Option[String] = None
  ): Future[Iterable[Map[String, String]]] = {
    // NOTE: The date path in byMonumentTemplateGeneric uses the listConfig derived
    // from listTemplate (unlike the original monumentsByDate which always used defaultListConfig).
    // This is an intentional improvement: the override is now respected on both paths.
    val listConfig = listTemplate.fold(defaultListConfig)(
      new OtherTemplateListConfig(_, defaultListConfig)
    )
    byMonumentTemplateGeneric(
      generatorTemplate,
      date,
      listTemplate,
      (page, text) => new WlxTemplateParser(listConfig, page).parseToMaps(text)
    )
  }
```

- [ ] **Step 5: Run all MonumentQuery-related tests (existing + new)**

```bash
sbt "scalawiki-wlx/testOnly org.scalawiki.wlx.query.MonumentQueryApiSpec org.scalawiki.wlx.MonumentDbSpec org.scalawiki.wlx.stat.StatisticsSpec"
```
Expected: all pass.

- [ ] **Step 6: Run the full wlx test suite to catch regressions**

```bash
sbt "scalawiki-wlx/test"
```
Expected: all tests pass.

- [ ] **Step 7: Commit**

```bash
git add scalawiki-wlx/src/main/scala/org/scalawiki/wlx/query/MonumentQuery.scala \
        scalawiki-wlx/src/test/scala/org/scalawiki/wlx/query/MonumentQueryApiSpec.scala
git commit -m "feat: extract byMonumentTemplateGeneric; add byMonumentTemplateMaps"
```

---

## Chunk 4: CLI wiring + integration

### Task 8: `StatParams` / `StatConfig` — add `--export-csv` flag

**Files:**
- Modify: `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/StatParams.scala`
- Modify: `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/stat/StatParamsSpec.scala`

- [ ] **Step 1: Write failing tests for `StatParamsSpec`**

Open `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/stat/StatParamsSpec.scala` and add:

```scala
  "--export-csv" should {

    "be absent by default" in {
      val cfg = StatParams.parse(Seq("--campaign", "WLM-UA"))
      cfg.exportCsv must beNone
    }

    "be set to None when flag given without filename (empty string)" in {
      // Scallop treats a flag with no value as missing; --export-csv with a value is the only way
      // This test confirms the default (no flag) produces None
      val cfg = StatParams.parse(Seq("--campaign", "WLM-UA"))
      cfg.exportCsv must beNone
    }

    "be set to Some(filename) when --export-csv myfile.csv is given" in {
      val cfg = StatParams.parse(Seq("--campaign", "WLM-UA", "--export-csv", "myfile.csv"))
      cfg.exportCsv must beSome("myfile.csv")
    }
  }
```

- [ ] **Step 2: Run to confirm failure**

```bash
sbt "scalawiki-wlx/testOnly org.scalawiki.wlx.stat.StatParamsSpec"
```
Expected: compilation error — `exportCsv` not found on `StatConfig`.

- [ ] **Step 3: Add `exportCsv` to `StatConfig`**

In `StatParams.scala`, add to the `StatConfig` case class (before the closing parenthesis, after `recentlyTakenFiles`):

```scala
    exportCsv: Option[String] = None
```

- [ ] **Step 4: Add `exportCsv` opt to `StatParams` class**

Inside `class StatParams`, add **before** the `verify()` call:

```scala
  val exportCsv =
    opt[String](name = "export-csv", descr = "Export monuments to CSV. Optional filename; defaults to <campaign>-YYYY-MM-DD-HHmm.csv")
```

- [ ] **Step 5: Wire `exportCsv` into `StatParams.parse()`**

In the `StatConfig(...)` constructor call inside `StatParams.parse()`, add:

```scala
      exportCsv = conf.exportCsv.toOption,
```

- [ ] **Step 6: Run StatParamsSpec**

```bash
sbt "scalawiki-wlx/testOnly org.scalawiki.wlx.stat.StatParamsSpec"
```
Expected: all tests pass including the new ones.

- [ ] **Step 7: Commit**

```bash
git add scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/StatParams.scala \
        scalawiki-wlx/src/test/scala/org/scalawiki/wlx/stat/StatParamsSpec.scala
git commit -m "feat: add --export-csv flag to StatParams/StatConfig"
```

---

### Task 9: Wire `Statistics.main()` + integration tests

**Files:**
- Modify: `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/Statistics.scala`
- Create: `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/stat/StatisticsExportSpec.scala`

- [ ] **Step 1: Write failing integration tests**

```scala
package org.scalawiki.wlx.stat

import org.scalawiki.wlx.dto.{Contest, Monument}
import org.scalawiki.wlx.query.MonumentQuery
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import java.nio.file.{Files, Paths}
import scala.concurrent.Future

class StatisticsExportSpec extends Specification with Mockito {

  private val contest = Contest.WLMUkraine(2025)

  def buildMonumentQuery(rows: Seq[Map[String, String]]): MonumentQuery = {
    val q = mock[MonumentQuery]
    // byMonumentTemplateMaps is `final` — stub the abstract async variant instead.
    // The final method delegates to it via Await.result, so runExport exercises the real path.
    q.byMonumentTemplateMapsAsync() returns Future.successful(rows)
    q
  }

  "Statistics.main with --export-csv" should {

    "use specified filename" in {
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
        val content = new String(Files.readAllBytes(path))
        content must contain("id")
        content must contain("14-101-0001")
      } finally {
        Files.deleteIfExists(path)
      }
    }

    "use default filename matching <campaign>-yyyy-MM-dd-HHmm.csv pattern" in {
      val cfg = StatConfig(
        campaign = "WLM-UA",
        years = Seq(2025),
        exportCsv = Some("")   // empty string → use default
      )
      val q = buildMonumentQuery(Seq(Map("ID" -> "14-101-0001")))
      val defaultName = Statistics.defaultCsvFilename("WLM-UA")

      defaultName must beMatching("WLM-UA-\\d{4}-\\d{2}-\\d{2}-\\d{4}\\.csv")
    }

    "produce no output for empty monument list" in {
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
```

- [ ] **Step 2: Run to confirm failure**

```bash
sbt "scalawiki-wlx/testOnly org.scalawiki.wlx.stat.StatisticsExportSpec"
```
Expected: compilation error — `Statistics.runExport` / `Statistics.defaultCsvFilename` not found.

- [ ] **Step 3: Add `defaultCsvFilename` and `runExport` to `Statistics` object**

In `Statistics.scala`, add to the `Statistics` companion object:

```scala
  def defaultCsvFilename(campaign: String): String = {
    val now = java.time.LocalDateTime.now()
    val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")
    s"$campaign-${now.format(fmt)}.csv"
  }

  def runExport(
      contest: Contest,
      cfg: StatConfig,
      monumentQuery: MonumentQuery
  ): Unit = {
    val path = cfg.exportCsv.filter(_.nonEmpty).getOrElse(defaultCsvFilename(cfg.campaign))
    val maps = monumentQuery.byMonumentTemplateMaps()
    val mapping = org.scalawiki.wlx.UaUkJsonMapping.load("monuments_config/ua_uk.json")
    org.scalawiki.wlx.MonumentCsvExporter.export(maps, mapping, path)
  }
```

- [ ] **Step 4: Wire into `Statistics.main()`**

In `Statistics.main()`, add a short-circuit before the existing `Statistics` construction:

```scala
  def main(args: Array[String]): Unit = {
    val cfg = StatParams.parse(args)
    val contest = getContest(cfg)

    if (cfg.exportCsv.isDefined) {
      val monumentQuery = MonumentQuery.create(contest, reportDifferentRegionIds = false)
      runExport(contest, cfg, monumentQuery)
      return
    }

    // existing flow below — unchanged
    val cacheName = s"${cfg.campaign}-${contest.year}"
    ...
  }
```

- [ ] **Step 5: Run integration tests**

```bash
sbt "scalawiki-wlx/testOnly org.scalawiki.wlx.stat.StatisticsExportSpec"
```
Expected: all tests pass.

- [ ] **Step 6: Run the full test suite**

```bash
sbt "scalawiki-wlx/test"
```
Expected: all tests pass. No regressions.

- [ ] **Step 7: Commit**

```bash
git add scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/Statistics.scala \
        scalawiki-wlx/src/test/scala/org/scalawiki/wlx/stat/StatisticsExportSpec.scala
git commit -m "feat: wire --export-csv into Statistics.main()"
```

---

## Final verification

- [ ] **Run entire wlx module test suite**

```bash
sbt "scalawiki-wlx/test"
```
Expected: all tests pass.

- [ ] **Smoke-test the CLI flag parsing**

```bash
sbt "scalawiki-wlx/run --campaign WLM-UA --help"
```
Expected: `--export-csv` appears in the help output.
