# Image CSV Export Design

**Date:** 2026-03-24
**Branch:** export-monuments
**Status:** Approved

## Overview

Export WLM contest image data to CSV files, one file per contest year. Runs as part of the normal statistics flow, triggered by a new `--export-images-csv` CLI flag.

## CLI Changes

Add `--export-images-csv <dir>` to `StatParams` and `StatConfig`:

```scala
// StatParams
val exportImagesCsv = opt[String](
  name = "export-images-csv",
  descr = "Export images to CSV files per year. Argument is output directory (default: current dir)."
)

// StatConfig
exportImagesCsv: Option[String] = None
```

- Argument is the output directory (empty string = current directory).
- Independent of the existing `--export-csv` (monument export) flag — both can coexist.
- Wired in `StatParams.parse()` via `conf.exportImagesCsv.toOption`.

## `ImageCsvExporter` Object

New file: `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/ImageCsvExporter.scala`

### Columns (in order)

| Column | Source |
|--------|--------|
| `title` | `image.title` |
| `author` | `image.author` |
| `upload_date` | `image.date` |
| `monument_id` | `image.monumentIds` (`;`-joined) |
| `page_id` | `image.pageId` |
| `width` | `image.width` |
| `height` | `image.height` |
| `size_bytes` | `image.size` |
| `mime` | `image.mime` |
| `camera` | `image.metadata.flatMap(_.camera)` |
| `exif_date` | `image.metadata.flatMap(_.date)` |
| `categories` | `image.categories` (`;`-joined) |
| `special_nominations` | `image.specialNominations` (`;`-joined) |
| `url` | `image.url` |
| `page_url` | `image.pageUrl` |

Multi-value fields (`monumentIds`, `categories`, `specialNominations`) are joined with `;`.

### Filename Convention

- **Previous years:** `<campaign>-<year>-images.csv`
  e.g. `WLM-UA-2024-images.csv`
- **Current year:** `<campaign>-<year>-<MM>-<dd>-<HHmm>.csv`
  e.g. `WLM-UA-2025-03-24-1430.csv`
- Output directory prepended when non-empty:
  e.g. `output/WLM-UA-2024-images.csv`

"Current year" is defined as `contest.year` (the last year in the configured range).

### Interface

```scala
object ImageCsvExporter {
  def export(
    imageDb: ImageDB,
    campaign: String,
    isCurrent: Boolean,
    outputDir: String
  ): Unit
}
```

Uses `com.github.tototoshi.csv.CSVWriter` (same dependency as `MonumentCsvExporter`).

## Wiring

In `ReporterRegistry.output()`, after `currentYear()` and `allYears()`:

```scala
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
```

`stat.dbsByYear` is always populated (even for single-year runs), so no special casing is needed. When no other stat flags are set, other reporters are no-ops and only the CSVs are written.

## Data Flow

```
Statistics.main(args)
  └─ StatParams.parse(args) → StatConfig(exportImagesCsv = Some(dir))
  └─ Statistics.init(total)
       └─ gatherData() → ContestStat(dbsByYear = [ImageDB(2022), ..., ImageDB(2025)])
       └─ ReporterRegistry.output()
            ├─ currentYear()   (other reporters, no-ops if no flags)
            ├─ allYears()      (other reporters, no-ops if no flags)
            └─ exportImagesCsv.foreach { dir =>
                 dbsByYear.foreach { imageDb =>
                   ImageCsvExporter.export(imageDb, campaign, isCurrent, dir)
                 }
               }
```

## Out of Scope

- Filtering images before export (ineligible, region, etc.) — exports `imageDb.images` as-is
- Column selection at runtime
- Monument CSV export changes (`--export-csv` is unchanged)
