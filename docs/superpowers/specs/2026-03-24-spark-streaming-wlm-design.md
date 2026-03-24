# Spark Streaming WLM Images Design

**Date:** 2026-03-24
**Status:** Approved

## Overview

A new `spark-streaming` sbt module that simulates a WLM image upload stream from exported CSV files and processes it with Spark Structured Streaming to compute the number of distinct monuments pictured per author and region — both as cumulative totals and within tumbling time windows.

## Module Structure

New sbt module `spark-streaming` at `spark-streaming/` inside the scalawiki project.

**No dependency on other scalawiki modules** — it reads exported CSVs as plain files and is fully self-contained.

**Dependencies:**
- `org.apache.spark` %% `spark-sql` (Spark 3.5, Scala 2.13)
- `org.apache.spark` %% `spark-core` (Spark 3.5)
- `com.holdenkarau` %% `spark-testing-base` (test scope)
- `org.scalatest` %% `scalatest` (test scope)

Assembly plugin configured for fat JAR deployment.

**Main classes:**
| Class | Responsibility |
|-------|---------------|
| `ImageUploadSimulator` | Copies exported CSV files one-by-one into the watched input directory at a configurable interval (e.g. one file per 5 seconds), simulating an upload stream |
| `WlmStreamingApp` | Spark Structured Streaming application: reads from the input directory, runs two queries (cumulative + windowed), writes results to console and Parquet |

Configuration via `application.conf` (Typesafe Config): input directory, output directory, simulator interval, window duration, watermark duration.

## Input Schema and Data Transformation

### CSV Schema

Matches `ImageCsvExporter.columns` exactly:

```
title, author, upload_date, monument_id, page_id, width, height,
size_bytes, mime, camera, exif_date, categories, special_nominations,
url, page_url
```

Spark reads the input directory as a CSV stream with `header = true` and schema inference.

### Transformation Pipeline

Applied once to the raw stream; result is shared by both queries:

1. **Parse timestamp:** `upload_date` (ISO-8601 string) → `upload_date_ts: TimestampType` via `to_timestamp`. Nulls tolerated — rows without a valid date are excluded from the windowed query but included in the cumulative query.
2. **Explode monuments:** `split(monument_id, ";")` then `explode` → one row per monument-image pair. Column renamed to `monument`.
3. **Extract region:** `regexp_replace(monument, "-\\d+$", "")` strips the trailing `-NNNN` segment.
   - Example: `14-101-0001` → `14-101`
4. **Select:** retain `author`, `monument`, `region`, `upload_date_ts`.

## The Two Streaming Queries

### Query 1 — Cumulative (complete mode)

Groups by `(author, region)` and counts distinct monuments. Runs in **complete mode** — rewrites the full result table each micro-batch.

```scala
transformedStream
  .groupBy("author", "region")
  .agg(countDistinct("monument").as("monuments_pictured"))
```

**Sinks:**
- Console (`truncate = false`)
- Parquet files → `<outputDir>/cumulative/`

**Checkpoint:** `<checkpointDir>/cumulative/`

### Query 2 — Windowed (append mode + watermark)

Uses a configurable tumbling window on `upload_date_ts` (default: 10 minutes) with a configurable watermark (default: 2 minutes). Same `(author, region)` grouping within each window. Rows appear in output only once the window closes (append semantics). Rows with null `upload_date_ts` are excluded.

```scala
transformedStream
  .withWatermark("upload_date_ts", "2 minutes")
  .groupBy(window("upload_date_ts", "10 minutes"), "author", "region")
  .agg(countDistinct("monument").as("monuments_pictured"))
```

**Sinks:**
- Console (`truncate = false`)
- Parquet files → `<outputDir>/windowed/`

**Checkpoint:** `<checkpointDir>/windowed/`

## Data Flow

```
Exported CSV files (one per year)
        │
        ▼
ImageUploadSimulator
  copies files → input/
        │
        ▼
WlmStreamingApp (Spark Structured Streaming)
  reads input/ as CSV stream
        │
        ▼
  Transformation pipeline
  (parse timestamp → explode monuments → extract region)
        │
        ├──► Query 1 (complete mode)
        │      groupBy(author, region)
        │      countDistinct(monument)
        │         │
        │         ├──► console
        │         └──► output/cumulative/ (Parquet)
        │
        └──► Query 2 (windowed, append mode + watermark)
               groupBy(window, author, region)
               countDistinct(monument)
                  │
                  ├──► console
                  └──► output/windowed/ (Parquet)
```

## Testing

**Framework:** ScalaTest + `spark-testing-base` (`com.holdenkarau` %% `spark-testing-base`)

| Test class | Extends | What it tests |
|------------|---------|---------------|
| `TransformationsSpec` | `DataFrameSuiteBase` | Transformation pipeline on static DataFrames: `monument_id` splitting, region extraction, null `upload_date` handling, multi-monument rows produce one row per monument |
| `CumulativeQuerySpec` | `DataFrameSuiteBase` | Cumulative aggregation: correct `monuments_pictured` counts per `(author, region)` on known input |
| `WindowedQuerySpec` | `DataFrameSuiteBase` | Windowed aggregation: fixed timestamps verify counts appear in the correct window |
| `ImageUploadSimulatorSpec` | `AnyFunSpec` | Simulator copies files to target directory at expected pace; uses jimfs in-memory filesystem |

No live streaming integration test — unit tests cover all logic.

## Out of Scope

- Kafka or other message broker integration
- Real-time dashboards or UI
- Monument name lookup (region code only, not full name)
- Multi-language author deduplication
