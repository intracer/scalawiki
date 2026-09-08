# Image CSV Export Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `--export-images-csv <dir>` flag that writes one CSV file per contest year to the given directory, each containing full image metadata for that year's WLM contest images.

**Architecture:** New `ImageCsvExporter` object (mirrors existing `MonumentCsvExporter`) is wired into `ReporterRegistry.output()`. `StatConfig`/`StatParams` gain a new `exportImagesCsv` field. `Statistics.main()` guard is updated so both `--export-csv` and `--export-images-csv` can coexist.

**Tech Stack:** Scala 2.13, sbt, Specs2 (tests), `com.github.tototoshi.csv.CSVWriter/CSVReader` (already in scope), `scalawiki-wlx` module.

---

## File Map

| Action | File |
|--------|------|
| **Create** | `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/ImageCsvExporter.scala` |
| **Create** | `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/ImageCsvExporterSpec.scala` |
| **Modify** | `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/StatParams.scala` |
| **Modify** | `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/stat/StatParamsSpec.scala` |
| **Modify** | `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/reports/ReporterRegistry.scala` |
| **Modify** | `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/Statistics.scala` |

---

## Task 1: Add `exportImagesCsv` to `StatConfig` and `StatParams`

**Files:**
- Modify: `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/StatParams.scala`
- Modify: `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/stat/StatParamsSpec.scala`

- [ ] **Step 1: Write failing tests in `StatParamsSpec`**

Add a new `"--export-images-csv"` block at the end of `StatParamsSpec.scala`:

```scala
"--export-images-csv" should {

  "be absent by default" in {
    val cfg = StatParams.parse(Seq("--campaign", "WLM-UA"))
    cfg.exportImagesCsv must beNone
  }

  "be set to Some(dir) when --export-images-csv output is given" in {
    val cfg = StatParams.parse(Seq("--campaign", "WLM-UA", "--export-images-csv", "output"))
    cfg.exportImagesCsv must beSome("output")
  }

  "be set to Some(empty string) when --export-images-csv is given with empty string" in {
    val cfg = StatParams.parse(Seq("--campaign", "WLM-UA", "--export-images-csv", ""))
    cfg.exportImagesCsv must beSome("")
  }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
sbt "scalawiki-wlx/testOnly *StatParamsSpec"
```

Expected: compilation failure or test failure mentioning `exportImagesCsv` not found.

- [ ] **Step 3: Add `exportImagesCsv` to `StatConfig`**

In `StatParams.scala`, add `exportImagesCsv: Option[String] = None` as the last field of the `StatConfig` case class (after `exportCsv`):

```scala
case class StatConfig(
    // ... all existing fields unchanged ...
    exportCsv: Option[String] = None,
    exportImagesCsv: Option[String] = None   // ← add this
)
```

- [ ] **Step 4: Add `--export-images-csv` option to `StatParams`**

In the `StatParams` class body, after the `exportCsv` opt:

```scala
val exportImagesCsv =
  opt[String](name = "export-images-csv", descr = "Export images to CSV files per year. Argument is output directory (default: current dir).")
```

- [ ] **Step 5: Wire in `StatParams.parse()`**

In `StatParams.parse()`, add to the `StatConfig(...)` constructor call (after `exportCsv = ...`):

```scala
exportImagesCsv = conf.exportImagesCsv.toOption
```

- [ ] **Step 6: Run tests to verify they pass**

```bash
sbt "scalawiki-wlx/testOnly *StatParamsSpec"
```

Expected: all tests pass.

- [ ] **Step 7: Commit**

```bash
git add scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/StatParams.scala \
        scalawiki-wlx/src/test/scala/org/scalawiki/wlx/stat/StatParamsSpec.scala
git commit -m "feat: add --export-images-csv flag to StatParams/StatConfig"
```

---

## Task 2: Implement `ImageCsvExporter`

**Files:**
- Create: `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/ImageCsvExporter.scala`
- Create: `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/ImageCsvExporterSpec.scala`

- [ ] **Step 1: Write failing tests**

Create `scalawiki-wlx/src/test/scala/org/scalawiki/wlx/ImageCsvExporterSpec.scala`:

```scala
package org.scalawiki.wlx

import com.github.tototoshi.csv.CSVReader
import org.scalawiki.dto.{Image, ImageMetadata}
import org.scalawiki.wlx.dto.Contest
import org.specs2.mutable.Specification

import java.io.StringReader
import java.nio.file.{Files, Path}
import java.time.ZonedDateTime

class ImageCsvExporterSpec extends Specification {

  // A contest year guaranteed to be in the past so isCurrent = false
  val prevContest: Contest = Contest.WLMUkraine(2022)
  // "Current" year contest — use isCurrent = true explicitly; year value doesn't matter for filename tests
  val curContest: Contest  = Contest.WLMUkraine(2022)

  val sampleDate: ZonedDateTime = ZonedDateTime.parse("2022-09-15T10:30:00Z")

  val fullImage: Image = Image(
    title            = "File:Test.jpg",
    author           = Some("AuthorName"),
    date             = Some(sampleDate),
    monumentIds      = Seq("14-101-0001", "14-101-0002"),
    pageId           = Some(123456L),
    url              = Some("https://upload.wikimedia.org/test.jpg"),
    pageUrl          = Some("https://commons.wikimedia.org/wiki/File:Test.jpg"),
    width            = Some(3000),
    height           = Some(2000),
    size             = Some(1048576L),
    mime             = Some("image/jpeg"),
    metadata         = Some(ImageMetadata(Map(
      "Model"           -> "Canon EOS 5D",
      "DateTimeOriginal"-> "2022:09:15 10:30:00"
    ))),
    categories       = Set("WLM 2022", "Kyiv"),
    specialNominations = Set("WLM2022-UA-interior")
  )

  /** Write imageDb to a temp dir and return (dir path, CSV content). */
  def exportAndRead(
      images: Seq[Image],
      contest: Contest,
      isCurrent: Boolean
  ): (Path, String) = {
    val dir = Files.createTempDirectory("image-csv-spec")
    val imageDb = new ImageDB(contest, images, None)
    ImageCsvExporter.export(imageDb, contest.campaign, isCurrent, dir.toString)
    val expectedName = ImageCsvExporter.filename(contest.campaign, contest.year, isCurrent, dir.toString)
    val content = new String(Files.readAllBytes(java.nio.file.Paths.get(expectedName)), "UTF-8")
    (dir, content)
  }

  def parseCsv(content: String): List[List[String]] = {
    val reader = CSVReader.open(new StringReader(content))
    try reader.all() finally reader.close()
  }

  "ImageCsvExporter.filename" should {

    "produce <campaign>-<year>-images.csv for a previous year (outputDir non-empty)" in {
      val name = ImageCsvExporter.filename("WLM-UA", 2022, isCurrent = false, outputDir = "output")
      name must_== "output/WLM-UA-2022-images.csv"
    }

    "produce bare <campaign>-<year>-images.csv when outputDir is empty" in {
      val name = ImageCsvExporter.filename("WLM-UA", 2022, isCurrent = false, outputDir = "")
      name must_== "WLM-UA-2022-images.csv"
    }

    "produce <campaign>-<year>-MM-dd-HHmm.csv for the current year" in {
      val name = ImageCsvExporter.filename("WLM-UA", 2025, isCurrent = true, outputDir = "")
      name must beMatching("WLM-UA-2025-\\d{2}-\\d{2}-\\d{4}\\.csv")
    }

    "include outputDir prefix for current year when outputDir is non-empty" in {
      val name = ImageCsvExporter.filename("WLM-UA", 2025, isCurrent = true, outputDir = "out")
      name must startWith("out/WLM-UA-2025-")
    }
  }

  "ImageCsvExporter.export" should {

    "write all 15 expected header columns in the correct order" in {
      val (_, content) = exportAndRead(Seq(fullImage), prevContest, isCurrent = false)
      val header = parseCsv(content).head
      header must_== List(
        "title", "author", "upload_date", "monument_id", "page_id",
        "width", "height", "size_bytes", "mime", "camera", "exif_date",
        "categories", "special_nominations", "url", "page_url"
      )
    }

    "serialize a full image row correctly" in {
      val (_, content) = exportAndRead(Seq(fullImage), prevContest, isCurrent = false)
      val rows = parseCsv(content)
      val header = rows.head
      val row    = rows(1)
      def col(name: String) = row(header.indexOf(name))

      col("title")         must_== "File:Test.jpg"
      col("author")        must_== "AuthorName"
      col("upload_date")   must_== sampleDate.toString
      col("monument_id")   must_== "14-101-0001;14-101-0002"
      col("page_id")       must_== "123456"
      col("width")         must_== "3000"
      col("height")        must_== "2000"
      col("size_bytes")    must_== "1048576"
      col("mime")          must_== "image/jpeg"
      col("camera")        must_== "Canon EOS 5D"
      col("url")           must_== "https://upload.wikimedia.org/test.jpg"
      col("page_url")      must_== "https://commons.wikimedia.org/wiki/File:Test.jpg"
    }

    "serialize multi-value fields with semicolon separator" in {
      val (_, content) = exportAndRead(Seq(fullImage), prevContest, isCurrent = false)
      val rows   = parseCsv(content)
      val header = rows.head
      val row    = rows(1)
      def col(name: String) = row(header.indexOf(name))

      col("monument_id").split(";").toSet must_== Set("14-101-0001", "14-101-0002")
      col("categories").split(";").toSet  must_== Set("WLM 2022", "Kyiv")
      col("special_nominations")          must_== "WLM2022-UA-interior"
    }

    "serialize missing Optional fields as empty strings" in {
      val minimal = Image("File:Minimal.jpg")
      val (_, content) = exportAndRead(Seq(minimal), prevContest, isCurrent = false)
      val rows   = parseCsv(content)
      val header = rows.head
      val row    = rows(1)
      def col(name: String) = row(header.indexOf(name))

      col("author")      must_== ""
      col("upload_date") must_== ""
      col("monument_id") must_== ""
      col("page_id")     must_== ""
      col("camera")      must_== ""
      col("exif_date")   must_== ""
      col("url")         must_== ""
      col("page_url")    must_== ""
    }

    "write no file when images is empty" in {
      val dir = Files.createTempDirectory("image-csv-empty")
      val imageDb = new ImageDB(prevContest, Seq.empty, None)
      ImageCsvExporter.export(imageDb, "WLM-UA", isCurrent = false, outputDir = dir.toString)
      val files = dir.toFile.listFiles()
      files must beNull.or(have size 0)
    }
  }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
sbt "scalawiki-wlx/testOnly *ImageCsvExporterSpec"
```

Expected: compilation failure — `ImageCsvExporter` not found.

- [ ] **Step 3: Implement `ImageCsvExporter`**

Create `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/ImageCsvExporter.scala`:

```scala
package org.scalawiki.wlx

import com.github.tototoshi.csv.CSVWriter
import org.scalawiki.dto.Image

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ImageCsvExporter {

  val columns: Seq[String] = Seq(
    "title", "author", "upload_date", "monument_id", "page_id",
    "width", "height", "size_bytes", "mime", "camera", "exif_date",
    "categories", "special_nominations", "url", "page_url"
  )

  def imageToRow(image: Image): Map[String, String] = Map(
    "title"              -> image.title,
    "author"             -> image.author.getOrElse(""),
    "upload_date"        -> image.date.map(_.toString).getOrElse(""),
    "monument_id"        -> image.monumentIds.mkString(";"),
    "page_id"            -> image.pageId.map(_.toString).getOrElse(""),
    "width"              -> image.width.map(_.toString).getOrElse(""),
    "height"             -> image.height.map(_.toString).getOrElse(""),
    "size_bytes"         -> image.size.map(_.toString).getOrElse(""),
    "mime"               -> image.mime.getOrElse(""),
    "camera"             -> image.metadata.flatMap(_.camera).getOrElse(""),
    "exif_date"          -> image.metadata.flatMap(_.date).map(_.toString).getOrElse(""),
    "categories"         -> image.categories.mkString(";"),
    "special_nominations"-> image.specialNominations.mkString(";"),
    "url"                -> image.url.getOrElse(""),
    "page_url"           -> image.pageUrl.getOrElse("")
  )

  def filename(
      campaign: String,
      contestYear: Int,
      isCurrent: Boolean,
      outputDir: String
  ): String = {
    val name = if (isCurrent) {
      val fmt = DateTimeFormatter.ofPattern("MM-dd-HHmm")
      s"$campaign-$contestYear-${LocalDateTime.now().format(fmt)}.csv"
    } else {
      s"$campaign-$contestYear-images.csv"
    }
    if (outputDir.nonEmpty) s"$outputDir/$name" else name
  }

  def export(
      imageDb: ImageDB,
      campaign: String,
      isCurrent: Boolean,
      outputDir: String
  ): Unit = {
    val images = imageDb.images.toSeq
    if (images.isEmpty) return

    val path = filename(campaign, imageDb.contest.year, isCurrent, outputDir)
    val writer = CSVWriter.open(new File(path), "UTF-8")
    try {
      writer.writeRow(columns)
      images.foreach { image =>
        val row = imageToRow(image)
        writer.writeRow(columns.map(c => row.getOrElse(c, "")))
      }
    } finally {
      writer.close()
    }
  }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
sbt "scalawiki-wlx/testOnly *ImageCsvExporterSpec"
```

Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add scalawiki-wlx/src/main/scala/org/scalawiki/wlx/ImageCsvExporter.scala \
        scalawiki-wlx/src/test/scala/org/scalawiki/wlx/ImageCsvExporterSpec.scala
git commit -m "feat: implement ImageCsvExporter with full image metadata columns"
```

---

## Task 3: Wire into `ReporterRegistry` and fix `Statistics.main()` guard

**Files:**
- Modify: `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/reports/ReporterRegistry.scala`
- Modify: `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/Statistics.scala`

- [ ] **Step 1: Add image CSV export to `ReporterRegistry.output()`**

In `ReporterRegistry.scala`, update the `output()` method. The current body is:

```scala
def output(): Unit = {
  currentYear()
  allYears()
}
```

Change it to:

```scala
def output(): Unit = {
  currentYear()
  allYears()

  cfg.exportImagesCsv.foreach { dir =>
    val currentYear = stat.contest.year
    stat.dbsByYear.foreach { imageDb =>
      ImageCsvExporter.export(
        imageDb,
        stat.contest.campaign,
        isCurrent = imageDb.contest.year == currentYear,
        outputDir = dir
      )
    }
  }
}
```

Also add the import at the top of the file:

```scala
import org.scalawiki.wlx.ImageCsvExporter
```

- [ ] **Step 2: Update `Statistics.main()` guard**

The current structure in `Statistics.main()` is:

```scala
if (cfg.exportCsv.isDefined) {
  val monumentQuery = MonumentQuery.create(contest)
  runExport(contest, cfg, monumentQuery)
} else {
  val cacheName = ...
  val imageQueryWiki = ...
  val stat = new Statistics(...)
  stat.init(total = cfg.years.size > 1)
}
```

Replace the `if/else` with two independent `if` blocks, keeping all existing `else`-branch code intact:

```scala
if (cfg.exportCsv.isDefined) {
  val monumentQuery = MonumentQuery.create(contest)
  runExport(contest, cfg, monumentQuery)
}

if (cfg.exportCsv.isEmpty || cfg.exportImagesCsv.isDefined) {
  val cacheName = s"${cfg.campaign}-${contest.year}"
  val imageQueryWiki = ImageQuery.create(
    new CachedBot(Site.ukWiki, cacheName + "-wiki", true, entries = 100)
  )

  val stat = new Statistics(
    contest,
    startYear = Some(cfg.years.head),
    monumentQuery = MonumentQuery.create(contest, reportDifferentRegionIds = true),
    config = Some(cfg),
    imageQuery = None,
    imageQueryWiki = Some(imageQueryWiki)
  )

  stat.init(total = cfg.years.size > 1)
}
```

- [ ] **Step 3: Compile the wlx module**

```bash
sbt "scalawiki-wlx/compile"
```

Expected: clean compile, no errors.

- [ ] **Step 4: Run all wlx tests**

```bash
sbt "scalawiki-wlx/test"
```

Expected: all tests pass (no regressions).

- [ ] **Step 5: Commit**

```bash
git add scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/reports/ReporterRegistry.scala \
        scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/Statistics.scala
git commit -m "feat: wire ImageCsvExporter into ReporterRegistry; fix Statistics.main guard"
```
