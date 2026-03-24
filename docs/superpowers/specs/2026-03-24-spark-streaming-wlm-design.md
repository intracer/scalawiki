# Spark Streaming WLM Images Design

**Date:** 2026-03-24
**Status:** Approved

## Overview

A new `spark-streaming` sbt module that simulates a WLM image upload stream from exported CSV files and processes it with Spark Structured Streaming to compute the number of distinct monuments pictured per author and region — both as cumulative totals and within tumbling time windows.

## Module Structure

New sbt module `spark-streaming` at `spark-streaming/` inside the scalawiki project.

**No dependency on other scalawiki modules** — it reads exported CSVs as plain files and is fully self-contained.

**Dependencies:**
- `org.apache.spark` %% `spark-sql` (Spark 3.5, Scala 2.13) — `spark-core` is a transitive dependency and need not be listed separately
- `com.holdenkarau` %% `spark-testing-base` % `3.5.6_2.1.3` (test scope)
- `org.scalatest` %% `scalatest` (test scope)
- `com.google.jimfs` % `jimfs` % `1.3.0` (test scope) — in-memory filesystem for `ImageUploadSimulatorSpec`

Assembly plugin configured for fat JAR deployment.

**Main classes:**
| Class | Responsibility |
|-------|---------------|
| `ImageUploadSimulator` | Copies exported CSV files one-by-one into the watched input directory at a configurable interval (e.g. one file per 5 seconds), simulating an upload stream |
| `WlmStreamingApp` | Spark Structured Streaming application: reads from the input directory, runs two queries (cumulative + windowed), writes results to console and Parquet |

Configuration via `application.conf` (Typesafe Config): input directory, output directory, checkpoint directory, simulator interval, window duration, watermark duration.

## Input Schema and Data Transformation

### CSV Schema

Matches `ImageCsvExporter.columns` exactly:

```
title, author, upload_date, monument_id, page_id, width, height,
size_bytes, mime, camera, exif_date, categories, special_nominations,
url, page_url
```

Spark reads the input directory as a CSV stream with `header = true` and an **explicit `StructType` schema** defined in `WlmStreamingApp`. Spark Structured Streaming requires an explicit schema for file-based streaming sources — `inferSchema` is not supported on streaming reads and raises an `AnalysisException` at startup. All 15 columns are declared as `StringType` except `upload_date` which is parsed in the transformation step.

### Transformation Pipeline

Applied once to the raw stream; result is shared by both queries:

1. **Parse timestamp:** `upload_date` (ISO-8601 string) → `upload_date_ts: TimestampType` via `to_timestamp`. Nulls tolerated — rows without a valid date are excluded from the windowed query but included in the cumulative query.
2. **Explode monuments:** `split(monument_id, ";")` then `explode` → one row per monument-image pair. Column renamed to `monument`. Rows where `monument_id` is null or empty are silently dropped by `explode` — this is accepted behavior (images with no monument association are excluded from all aggregations).
3. **Extract region:** `regexp_replace(monument, "-\\d+$", "")` strips the trailing `-NNNN` segment.
   - Example: `14-101-0001` → `14-101`
4. **Select:** retain `author`, `monument`, `region`, `upload_date_ts`.

## The Two Streaming Queries

### Query 1 — Cumulative (complete mode)

Groups by `(author, region)` and counts approximate distinct monuments. Runs in **complete mode** — rewrites the full result table each micro-batch.

Spark Structured Streaming does not support `countDistinct` on streaming sources; `approx_count_distinct` (HyperLogLog) is the supported approximate substitute.

```scala
transformedStream
  .groupBy("author", "region")
  .agg(approx_count_distinct("monument").as("monuments_pictured"))
```

**Sinks (one `StreamingQuery`):**

Query 1 is started as a single `StreamingQuery` using the `foreachBatch` sink. Inside the `foreachBatch` function, each micro-batch result DataFrame is written twice: once to the console (via `show(truncate = false)`) and once as Parquet to `<outputDir>/cumulative/` (overwrite mode). This single-query approach is required because Spark file sinks do not support complete output mode directly — `foreachBatch` receives the full result DataFrame each micro-batch and writes it as a regular batch operation.

**Checkpoint:** `<checkpointDir>/cumulative/`

### Query 2 — Windowed (append mode + watermark)

Uses a configurable tumbling window on `upload_date_ts` (default: 10 minutes) with a configurable watermark (default: 2 minutes). Same `(author, region)` grouping within each window. Rows appear in output only once the window closes (append semantics). Rows with null `upload_date_ts` are excluded.

```scala
transformedStream
  .withWatermark("upload_date_ts", "2 minutes")
  .groupBy(window("upload_date_ts", "10 minutes"), "author", "region")
  .agg(approx_count_distinct("monument").as("monuments_pictured"))
```

**Sinks (one `StreamingQuery`):**

Query 2 is also started as a single `StreamingQuery` using `foreachBatch`. The outer `writeStream` must declare `.outputMode("append")` to match the windowed aggregation with watermark. Inside the `foreachBatch` function, each micro-batch result DataFrame is written twice: once to the console (via `show(truncate = false)`) and once as Parquet to `<outputDir>/windowed/`. Parquet file sinks do support append mode natively in Spark Structured Streaming, but using `foreachBatch` mirrors the Query 1 pattern and avoids managing two separate `StreamingQuery` handles for the same aggregation.

```scala
windowedAgg
  .writeStream
  .outputMode("append")
  .option("checkpointLocation", s"$checkpointDir/windowed")
  .foreachBatch { (batchDf: DataFrame, _: Long) =>
    batchDf.show(truncate = false)
    batchDf.write.mode("append").parquet(s"$outputDir/windowed")
  }
  .start()
```

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
        ├──► Query 1 (complete mode, single StreamingQuery via foreachBatch)
        │      groupBy(author, region)
        │      approx_count_distinct(monument)
        │         │
        │         └──► foreachBatch { batchDf =>
        │                batchDf.show(truncate=false)          // console
        │                batchDf.write.parquet(cumulative/)    // Parquet
        │              }
        │
        └──► Query 2 (windowed, append mode + watermark, single StreamingQuery via foreachBatch)
               groupBy(window, author, region)
               approx_count_distinct(monument)
                  │
                  └──► foreachBatch { batchDf =>
                         batchDf.show(truncate=false)         // console
                         batchDf.write.parquet(windowed/)     // Parquet
                       }
```

## Testing

**Framework:** ScalaTest + `spark-testing-base` (`com.holdenkarau` %% `spark-testing-base` % `3.5.6_2.1.3`). `DataFrameSuiteBase` and `StreamingSuiteBase` manage `SparkSession` lifecycle automatically.

### `MemoryStream` encoder note (Spark 3.5)

`MemoryStream[Row]` requires an implicit `Encoder[Row]`. In Spark 3.5, `RowEncoder.apply(schema)` was removed; use `RowEncoder.encoderFor(schema)`:

```scala
implicit val encoder: Encoder[Row] = RowEncoder.encoderFor(schema)
implicit val sqlContext: SQLContext = spark.sqlContext
val memStream = MemoryStream[Row]
```

### `WindowedQuerySpec` test wiring

`WindowedQuerySpec` builds a standalone test query — it does not invoke `WlmStreamingApp` directly:

1. Create `MemoryStream[Row]` with the transformed stream schema (`author`, `monument`, `region`, `upload_date_ts`) using `RowEncoder.encoderFor` as above.
2. Apply the windowed aggregation to `memStream.toDF()` (same logic as production).
3. Start the query with a memory sink: `.writeStream.format("memory").queryName("windowed_test").outputMode("append").start()`.
4. Inject rows via `memStream.addData(rows)`, then call `query.processAllAvailable()`.
5. To force a window to close, include a sentinel row timestamped at least `window_end + watermark_duration` after the last in-window event — without it the watermark will not advance and results will be empty. Example: to close a 10-minute window ending at T+10 with a 2-minute watermark, add a row at T+12 or later.
6. Read results from `spark.table("windowed_test")` and assert expected `(window, author, region, monuments_pictured)` tuples.
7. Verify late rows (older than the current watermark) do not appear.

| Test class | Extends | What it tests |
|------------|---------|---------------|
| `TransformationsSpec` | `DataFrameSuiteBase` | Transformation pipeline on static DataFrames: `monument_id` splitting, region extraction, null `upload_date` handling, multi-monument rows produce one row per monument |
| `CumulativeQuerySpec` | `DataFrameSuiteBase` | Cumulative aggregation: runs `approx_count_distinct` on a **static DataFrame** (not a streaming source) to verify correct `monuments_pictured` counts per `(author, region)` on known input |
| `WindowedQuerySpec` | `StreamingSuiteBase` | Windowed aggregation with append mode: standalone test query using `MemoryStream[Row]` as source and `format("memory")` as sink, as described above; fixed timestamps verify correct window bucketing; late rows excluded |
| `ImageUploadSimulatorSpec` | `AnyFunSpec` | Simulator copies files to target directory at expected pace; `ImageUploadSimulator` accepts a `java.nio.file.FileSystem` parameter for injection; uses jimfs in-memory filesystem in tests |

No live streaming integration test — unit tests cover all logic.

## Out of Scope

- Kafka or other message broker integration
- Real-time dashboards or UI
- Monument name lookup (region code only, not full name)
- Multi-language author deduplication
