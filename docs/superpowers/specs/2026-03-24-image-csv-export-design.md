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
- Takes a **directory** (not a filename), because multiple files are written (one per year). This differs intentionally from `--export-csv` which takes a single filename.

## `ImageCsvExporter` Object

New file: `scalawiki-wlx/src/main/scala/org/scalawiki/wlx/ImageCsvExporter.scala`

### Columns (in order)

| Column | Source | Notes |
|--------|--------|-------|
| `title` | `image.title` | |
| `author` | `image.author` | |
| `upload_date` | `image.date` | ISO-8601 string (`.toString` on `ZonedDateTime`) |
| `monument_id` | `image.monumentIds` | `;`-joined |
| `page_id` | `image.pageId` | |
| `width` | `image.width` | |
| `height` | `image.height` | |
| `size_bytes` | `image.size` | |
| `mime` | `image.mime` | |
| `camera` | `image.metadata.flatMap(_.camera)` | |
| `exif_date` | `image.metadata.flatMap(_.date)` | ISO-8601 string (`.toString` on `ZonedDateTime`) |
| `categories` | `image.categories` | `;`-joined |
| `special_nominations` | `image.specialNominations` | `;`-joined |
| `url` | `image.url` | |
| `page_url` | `image.pageUrl` | |

All `Option` values serialize to empty string when `None`. Both `upload_date` and `exif_date` are `Option[ZonedDateTime]`; use `.toString` (ISO-8601) when present.

### Empty image sets

If `imageDb.images` is empty, the exporter writes nothing (mirrors `MonumentCsvExporter` behaviour).

### Filename Convention

- **Previous years:** `<campaign>-<year>-images.csv`
  e.g. `WLM-UA-2024-images.csv`
- **Current year:** `<campaign>-<contestYear>-<MM>-<dd>-<HHmm>.csv`
  where `<contestYear>` is the contest year and `<MM>-<dd>-<HHmm>` is the run timestamp from `LocalDateTime.now()`.
  e.g. contest year 2025, run on 2026-03-24 at 14:30 → `WLM-UA-2025-03-24-1430.csv`
  Format string: `s"$campaign-$contestYear-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-HHmm"))}.csv"`
- Output directory prepended when non-empty; when empty, filename is written to the current working directory (no path prefix):
  ```scala
  if (outputDir.nonEmpty) s"$outputDir/$name" else name
  ```
  e.g. `output/WLM-UA-2024-images.csv` or `WLM-UA-2024-images.csv`

"Current year" is `stat.contest.year`, which equals `cfg.years.last` (set via `Statistics.getContest()`). All `ImageDB` instances in `dbsByYear` share the same campaign; `stat.contest.campaign` is the canonical source.

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

### `Statistics.main()` guard

Currently, `Statistics.main()` uses an if/else: when `cfg.exportCsv.isDefined` the stats/`ReporterRegistry` path is skipped entirely. This must be updated so that `--export-images-csv` always runs through the stats flow, even when `--export-csv` is also set:

```scala
if (cfg.exportCsv.isDefined) {
  runExport(contest, cfg, MonumentQuery.create(contest))  // monument CSV export
}

if (cfg.exportCsv.isEmpty || cfg.exportImagesCsv.isDefined) {
  // normal stats flow (includes image CSV export via ReporterRegistry)
  val stat = new Statistics(...)
  stat.init(total = cfg.years.size > 1)
}
```

This means both flags can be used together. If only `--export-csv` is set, the stats flow is still skipped (no change to existing behaviour).

### `ReporterRegistry.output()`

After `currentYear()` and `allYears()`:

```scala
cfg.exportImagesCsv.foreach { dir =>
  val currentYear = stat.contest.year  // = cfg.years.last
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

`stat.dbsByYear` is always populated: `Statistics` builds `contests` from `startYear to currentYear`, which always has at least one element. For a single-year run, `dbsByYear` contains exactly one `ImageDB`.

**Note:** When `--export-images-csv` is set without other stat flags, the full statistics flow still runs (including wiki image queries via `CachedBot`). This is accepted behaviour — image data must be fetched to populate `dbsByYear` regardless. The cache layer (`CachedBot`) mitigates repeated network overhead.

## Data Flow

```
Statistics.main(args)
  └─ StatParams.parse(args) → StatConfig(exportImagesCsv = Some(dir))
  └─ [if exportCsv set] runExport() → monument CSV (unchanged)
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
