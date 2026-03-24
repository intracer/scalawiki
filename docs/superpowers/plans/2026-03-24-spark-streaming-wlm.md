# Spark Streaming WLM Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a new `spark-streaming` sbt module that simulates a WLM image upload stream from exported CSV files and processes it with Spark Structured Streaming to compute monuments pictured per author and region, both cumulatively and in tumbling time windows.

**Architecture:** A self-contained sbt module with no scalawiki dependencies. Production logic is split across `WlmSchema` (CSV schema), `Transformations` (parse/explode/extract), and `Queries` (aggregation) so each can be unit-tested independently. `WlmStreamingApp` wires them together; `ImageUploadSimulator` feeds the watched input directory.

**Tech Stack:** Scala 2.13, Spark 3.5.6 (`spark-sql`), Typesafe Config, ScalaTest 3.2.x + `spark-testing-base` 3.5.6_2.1.3 (`DataFrameSuiteBase` / `StreamingSuiteBase`), jimfs 1.3.0 for filesystem injection in simulator tests, sbt-assembly for fat JAR.

---

## File Structure

| File | Status | Responsibility |
|------|--------|----------------|
| `build.sbt` | Modify | Add `spark-streaming` module definition and root aggregate |
| `project/Dependencies.scala` | Modify | Add `SparkV`, `SparkTestingBaseV`, `ScalaTestV` version constants |
| `spark-streaming/src/main/resources/application.conf` | Create | Default config: dirs, interval, window/watermark durations |
| `spark-streaming/src/main/scala/org/scalawiki/spark/WlmSchema.scala` | Create | `csvSchema: StructType` (15 cols) and `transformedSchema: StructType` (4 cols) |
| `spark-streaming/src/main/scala/org/scalawiki/spark/Transformations.scala` | Create | `transform(df: DataFrame): DataFrame` — parse timestamp, explode monuments, extract region |
| `spark-streaming/src/main/scala/org/scalawiki/spark/Queries.scala` | Create | `cumulativeAgg(df)` and `windowedAgg(df, windowDur, watermark)` |
| `spark-streaming/src/main/scala/org/scalawiki/spark/WlmStreamingApp.scala` | Create | Main app: reads CSV stream, starts two `foreachBatch` queries, `awaitAnyTermination()` |
| `spark-streaming/src/main/scala/org/scalawiki/spark/ImageUploadSimulator.scala` | Create | Copies files into input dir one-by-one at a configurable interval; accepts `FileSystem` for testability |
| `spark-streaming/src/test/resources/log4j2.properties` | Create | Suppress Spark log noise during tests |
| `spark-streaming/src/test/scala/org/scalawiki/spark/TransformationsSpec.scala` | Create | `DataFrameSuiteBase` — static DataFrame tests for transform pipeline |
| `spark-streaming/src/test/scala/org/scalawiki/spark/CumulativeQuerySpec.scala` | Create | `DataFrameSuiteBase` — `approx_count_distinct` on static DataFrame |
| `spark-streaming/src/test/scala/org/scalawiki/spark/WindowedQuerySpec.scala` | Create | `StreamingSuiteBase` — `MemoryStream[Row]` + memory sink + watermark sentinel |
| `spark-streaming/src/test/scala/org/scalawiki/spark/ImageUploadSimulatorSpec.scala` | Create | `AnyFunSuite` — jimfs in-memory filesystem, timing verification (uses `test(...)` syntax; spec table says `AnyFunSpec` but `AnyFunSuite` is the correct match for the test body here) |

---

## Task 1: sbt Module Scaffold

**Files:**
- Modify: `project/Dependencies.scala`
- Modify: `build.sbt`

- [ ] **Step 1: Add version constants to `project/Dependencies.scala`**

  Add these three lines inside the `object Dependencies` body, after existing version vals:

  ```scala
  val ScalaTestV        = "3.2.19"
  val SparkV            = "3.5.6"
  val SparkTestingBaseV = "3.5.6_2.1.3"
  ```

- [ ] **Step 2: Add `spark-streaming` module to `build.sbt`**

  Add this block at the end of `build.sbt`, before the closing of the file:

  ```scala
  lazy val `spark-streaming` = Project("spark-streaming", file("spark-streaming"))
    .settings(commonSettings: _*)
    .settings(
      // commonSettings injects Specs2 + MockServer into every module's test classpath.
      // Strip them here — this module uses ScalaTest and spark-testing-base only.
      libraryDependencies := libraryDependencies.value.filterNot { m =>
        m.organization == "org.specs2" || m.organization == "org.mock-server"
      },
      libraryDependencies ++= Seq(
        "org.apache.spark"  %% "spark-sql"           % SparkV,
        "com.typesafe"       % "config"               % TypesafeConfigV,
        "com.holdenkarau"   %% "spark-testing-base"  % SparkTestingBaseV % Test,
        "org.scalatest"     %% "scalatest"            % ScalaTestV        % Test,
        "com.google.jimfs"   % "jimfs"                % JimFsV            % Test
      ),
      // ThisBuild sets fork := true globally. Spark tests need SPARK_LOCAL_IP to
      // avoid hostname resolution issues in forked JVMs. Set it here if tests hang.
      Test / envVars += "SPARK_LOCAL_IP" -> "127.0.0.1",
      Test / javaOptions ++= Seq("-Xmx2G", "-XX:+UseG1GC"),
      assembly / mainClass := Some("org.scalawiki.spark.WlmStreamingApp"),
      assembly / assemblyMergeStrategy := {
        case PathList("META-INF", "services", _*) => MergeStrategy.concat
        case PathList("META-INF", _*)             => MergeStrategy.discard
        case "reference.conf"                     => MergeStrategy.concat
        case _                                    => MergeStrategy.first
      }
    )
  ```

  Also add `spark-streaming` to the root project's `.aggregate(...)` call:

  ```scala
  lazy val scalawiki = (project in file("."))
    .settings(commonSettings)
    .dependsOn(core, bots, dumps, wlx, `http-extensions`)
    .aggregate(core, bots, dumps, wlx, `http-extensions`, `spark-streaming`)
  ```

- [ ] **Step 3: Create the source directories**

  ```bash
  mkdir -p spark-streaming/src/main/scala/org/scalawiki/spark
  mkdir -p spark-streaming/src/main/resources
  mkdir -p spark-streaming/src/test/scala/org/scalawiki/spark
  mkdir -p spark-streaming/src/test/resources
  ```

- [ ] **Step 4: Create a minimal placeholder to verify compilation**

  Create `spark-streaming/src/main/scala/org/scalawiki/spark/WlmStreamingApp.scala`:

  ```scala
  package org.scalawiki.spark

  object WlmStreamingApp {
    def main(args: Array[String]): Unit = println("WlmStreamingApp placeholder")
  }
  ```

- [ ] **Step 5: Verify the module compiles**

  Run: `sbt "spark-streaming/compile"`

  Expected: `[success]` — Spark dependencies resolve and the placeholder compiles.

- [ ] **Step 6: Commit**

  ```bash
  git add project/Dependencies.scala build.sbt spark-streaming/
  git commit -m "feat: scaffold spark-streaming sbt module with Spark 3.5 dependencies"
  ```

---

## Task 2: Schema + Transformation Pipeline

**Files:**
- Create: `spark-streaming/src/main/scala/org/scalawiki/spark/WlmSchema.scala`
- Create: `spark-streaming/src/main/scala/org/scalawiki/spark/Transformations.scala`
- Create: `spark-streaming/src/test/resources/log4j2.properties`
- Create: `spark-streaming/src/test/scala/org/scalawiki/spark/TransformationsSpec.scala`

- [ ] **Step 1: Create test log4j config to suppress Spark noise**

  Create `spark-streaming/src/test/resources/log4j2.properties`:

  ```properties
  rootLogger.level = warn
  appender.console.type = Console
  appender.console.name = ConsoleAppender
  appender.console.layout.type = PatternLayout
  appender.console.layout.pattern = %d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n
  rootLogger.appenderRef.console.ref = ConsoleAppender
  ```

- [ ] **Step 2: Write the failing tests in `TransformationsSpec.scala`**

  Create `spark-streaming/src/test/scala/org/scalawiki/spark/TransformationsSpec.scala`:

  ```scala
  package org.scalawiki.spark

  import com.holdenkarau.spark.testing.DataFrameSuiteBase
  import org.apache.spark.sql.Row
  import org.apache.spark.sql.types._
  import org.scalatest.funsuite.AnyFunSuite

  class TransformationsSpec extends AnyFunSuite with DataFrameSuiteBase {

    // Minimal input schema — only columns used by Transformations.transform
    private val inputSchema = StructType(Seq(
      StructField("author",      StringType),
      StructField("upload_date", StringType),
      StructField("monument_id", StringType)
    ))

    test("splits monument_id on semicolon producing one row per monument") {
      val input = spark.createDataFrame(
        spark.sparkContext.parallelize(Seq(
          Row("Alice", "2022-10-01T10:00:00Z", "14-101-0001;14-101-0002")
        )),
        inputSchema
      )
      val result = Transformations.transform(input)
      assert(result.count() == 2)
      val monuments = result.select("monument").collect().map(_.getString(0)).toSet
      assert(monuments == Set("14-101-0001", "14-101-0002"))
    }

    test("extracts region by stripping trailing -NNNN segment") {
      val input = spark.createDataFrame(
        spark.sparkContext.parallelize(Seq(
          Row("Alice", "2022-10-01T10:00:00Z", "14-101-0001")
        )),
        inputSchema
      )
      val result = Transformations.transform(input)
      assert(result.collect()(0).getAs[String]("region") == "14-101")
    }

    test("null upload_date produces null upload_date_ts but row is kept") {
      val input = spark.createDataFrame(
        spark.sparkContext.parallelize(Seq(
          Row("Alice", null, "14-101-0001")
        )),
        inputSchema
      )
      val result = Transformations.transform(input)
      assert(result.count() == 1)
      assert(result.collect()(0).isNullAt(result.schema.fieldIndex("upload_date_ts")))
    }

    test("empty monument_id produces no rows") {
      val input = spark.createDataFrame(
        spark.sparkContext.parallelize(Seq(
          Row("Alice", "2022-10-01T10:00:00Z", ""),
          Row("Alice", "2022-10-01T10:00:00Z", null.asInstanceOf[String])
        )),
        inputSchema
      )
      val result = Transformations.transform(input)
      assert(result.count() == 0)
    }

    test("output schema is author, monument, region, upload_date_ts") {
      val input = spark.createDataFrame(
        spark.sparkContext.parallelize(Seq(
          Row("Alice", "2022-10-01T10:00:00Z", "14-101-0001")
        )),
        inputSchema
      )
      val result = Transformations.transform(input)
      val fields = result.schema.fieldNames.toSeq
      assert(fields == Seq("author", "monument", "region", "upload_date_ts"))
    }
  }
  ```

- [ ] **Step 3: Run tests to confirm they fail**

  Run: `sbt "spark-streaming/testOnly *TransformationsSpec"`

  Expected: compilation error — `Transformations` not found.

- [ ] **Step 4: Create `WlmSchema.scala`**

  Create `spark-streaming/src/main/scala/org/scalawiki/spark/WlmSchema.scala`:

  ```scala
  package org.scalawiki.spark

  import org.apache.spark.sql.types._

  object WlmSchema {

    /** Schema matching ImageCsvExporter.columns — all StringType; upload_date parsed in transform. */
    val csvSchema: StructType = StructType(Seq(
      StructField("title",               StringType),
      StructField("author",              StringType),
      StructField("upload_date",         StringType),
      StructField("monument_id",         StringType),
      StructField("page_id",             StringType),
      StructField("width",               StringType),
      StructField("height",              StringType),
      StructField("size_bytes",          StringType),
      StructField("mime",                StringType),
      StructField("camera",              StringType),
      StructField("exif_date",           StringType),
      StructField("categories",          StringType),
      StructField("special_nominations", StringType),
      StructField("url",                 StringType),
      StructField("page_url",            StringType)
    ))

    /** Output schema of Transformations.transform — fed into both streaming queries. */
    val transformedSchema: StructType = StructType(Seq(
      StructField("author",         StringType),
      StructField("monument",       StringType),
      StructField("region",         StringType),
      StructField("upload_date_ts", TimestampType)
    ))
  }
  ```

- [ ] **Step 5: Create `Transformations.scala`**

  Create `spark-streaming/src/main/scala/org/scalawiki/spark/Transformations.scala`:

  ```scala
  package org.scalawiki.spark

  import org.apache.spark.sql.DataFrame
  import org.apache.spark.sql.functions._

  object Transformations {

    /**
     * Transforms a raw WLM CSV DataFrame into one row per monument-image pair.
     *
     * Steps:
     *   1. Parse upload_date (ISO-8601 string) → upload_date_ts (TimestampType). Nulls tolerated.
     *   2. Split monument_id on ";" and explode → one row per monument. Empty/null monument_id
     *      rows are dropped (explode drops null arrays; empty strings are filtered explicitly).
     *   3. Extract region by stripping the trailing -NNNN segment from the monument id.
     *   4. Select author, monument, region, upload_date_ts.
     */
    def transform(df: DataFrame): DataFrame = {
      df.withColumn("upload_date_ts", to_timestamp(col("upload_date")))
        .withColumn("monument", explode(split(col("monument_id"), ";")))
        .filter(col("monument") =!= "")
        .withColumn("region", regexp_replace(col("monument"), "-\\d+$", ""))
        .select("author", "monument", "region", "upload_date_ts")
    }
  }
  ```

- [ ] **Step 6: Run tests and verify they pass**

  Run: `sbt "spark-streaming/testOnly *TransformationsSpec"`

  Expected: `5 tests, 0 failures`

- [ ] **Step 7: Commit**

  ```bash
  git add spark-streaming/src/
  git commit -m "feat: add WlmSchema and Transformations with tests"
  ```

---

## Task 3: Aggregation Queries + Cumulative Test

**Files:**
- Create: `spark-streaming/src/main/scala/org/scalawiki/spark/Queries.scala`
- Create: `spark-streaming/src/test/scala/org/scalawiki/spark/CumulativeQuerySpec.scala`

- [ ] **Step 1: Write the failing test in `CumulativeQuerySpec.scala`**

  Create `spark-streaming/src/test/scala/org/scalawiki/spark/CumulativeQuerySpec.scala`:

  ```scala
  package org.scalawiki.spark

  import com.holdenkarau.spark.testing.DataFrameSuiteBase
  import org.apache.spark.sql.Row
  import org.apache.spark.sql.types._
  import org.scalatest.funsuite.AnyFunSuite

  class CumulativeQuerySpec extends AnyFunSuite with DataFrameSuiteBase {

    // Static DataFrame with transformed schema — no streaming source needed.
    // approx_count_distinct is exact for small cardinalities so assertions are precise.
    private val schema = WlmSchema.transformedSchema

    test("counts distinct monuments per author+region") {
      val rows = spark.sparkContext.parallelize(Seq(
        Row("Alice", "14-101-0001", "14-101", null),
        Row("Alice", "14-101-0002", "14-101", null),
        Row("Alice", "14-101-0001", "14-101", null), // duplicate — same monument, not counted twice
        Row("Bob",   "14-102-0001", "14-102", null)
      ))
      val df = spark.createDataFrame(rows, schema)
      val result = Queries.cumulativeAgg(df)

      val alice = result.filter(result("author") === "Alice").collect()
      assert(alice.length == 1)
      assert(alice(0).getAs[Long]("monuments_pictured") == 2L)

      val bob = result.filter(result("author") === "Bob").collect()
      assert(bob.length == 1)
      assert(bob(0).getAs[Long]("monuments_pictured") == 1L)
    }

    test("groups by both author and region independently") {
      val rows = spark.sparkContext.parallelize(Seq(
        Row("Alice", "14-101-0001", "14-101", null),
        Row("Alice", "14-102-0001", "14-102", null)
      ))
      val df = spark.createDataFrame(rows, schema)
      val result = Queries.cumulativeAgg(df)
      assert(result.count() == 2)
    }
  }
  ```

- [ ] **Step 2: Run tests to confirm they fail**

  Run: `sbt "spark-streaming/testOnly *CumulativeQuerySpec"`

  Expected: compilation error — `Queries` not found.

- [ ] **Step 3: Create `Queries.scala`**

  Create `spark-streaming/src/main/scala/org/scalawiki/spark/Queries.scala`:

  ```scala
  package org.scalawiki.spark

  import org.apache.spark.sql.DataFrame
  import org.apache.spark.sql.functions._

  object Queries {

    /**
     * Cumulative aggregation: approximate distinct monuments per (author, region).
     * Uses approx_count_distinct (HyperLogLog) — countDistinct is not supported
     * on Spark Structured Streaming sources.
     * Designed for complete output mode.
     */
    def cumulativeAgg(df: DataFrame): DataFrame =
      df.groupBy("author", "region")
        .agg(approx_count_distinct("monument").as("monuments_pictured"))

    /**
     * Windowed aggregation: approximate distinct monuments per (window, author, region).
     * Uses a tumbling window on upload_date_ts with a watermark to bound state.
     * Designed for append output mode — windows appear in output only once closed.
     * Rows with null upload_date_ts are excluded (watermark column must be non-null).
     */
    def windowedAgg(df: DataFrame, windowDuration: String, watermarkDuration: String): DataFrame =
      df.withWatermark("upload_date_ts", watermarkDuration)
        .groupBy(window(col("upload_date_ts"), windowDuration), col("author"), col("region"))
        .agg(approx_count_distinct("monument").as("monuments_pictured"))
  }
  ```

- [ ] **Step 4: Run tests and verify they pass**

  Run: `sbt "spark-streaming/testOnly *CumulativeQuerySpec"`

  Expected: `2 tests, 0 failures`

- [ ] **Step 5: Commit**

  ```bash
  git add spark-streaming/src/
  git commit -m "feat: add Queries (cumulativeAgg, windowedAgg) with cumulative tests"
  ```

---

## Task 4: Windowed Query Tests

**Files:**
- Create: `spark-streaming/src/test/scala/org/scalawiki/spark/WindowedQuerySpec.scala`

`StreamingSuiteBase` from spark-testing-base provides `spark: SparkSession`. The test builds a standalone streaming query — it does NOT call `WlmStreamingApp`. Key Spark 3.5 note: `RowEncoder.apply(schema)` was removed; use `RowEncoder.encoderFor(schema)`.

To force a window to close in a watermarked append-mode query, you must inject a sentinel row timestamped at least `window_end + watermark_duration` after the last in-window event. Without it, `processAllAvailable()` returns an empty result table.

- [ ] **Step 1: Write the failing test in `WindowedQuerySpec.scala`**

  Create `spark-streaming/src/test/scala/org/scalawiki/spark/WindowedQuerySpec.scala`:

  ```scala
  package org.scalawiki.spark

  import com.holdenkarau.spark.testing.StreamingSuiteBase
  import org.apache.spark.sql.{Encoder, Row, SQLContext}
  import org.apache.spark.sql.catalyst.encoders.RowEncoder
  import org.apache.spark.sql.functions.col
  import org.scalatest.funsuite.AnyFunSuite

  import java.sql.Timestamp

  class WindowedQuerySpec extends AnyFunSuite with StreamingSuiteBase {

    private val schema = WlmSchema.transformedSchema
    implicit lazy val encoder: Encoder[Row] = RowEncoder.encoderFor(schema)

    test("emits counts for closed windows") {
      implicit val sqlContext: SQLContext = spark.sqlContext
      import org.apache.spark.sql.execution.streaming.MemoryStream

      val memStream = MemoryStream[Row]
      val windowed = Queries.windowedAgg(memStream.toDF(), "10 minutes", "2 minutes")

      val query = windowed.writeStream
        .format("memory")
        .queryName("windowed_test_closed")
        .outputMode("append")
        .start()

      // Two distinct monuments for Alice in the 10:00–10:10 window
      val t0 = Timestamp.valueOf("2022-10-01 10:00:00")
      val t1 = Timestamp.valueOf("2022-10-01 10:05:00")
      memStream.addData(
        Row("Alice", "14-101-0001", "14-101", t0),
        Row("Alice", "14-101-0002", "14-101", t1)
      )
      // Sentinel row at T+13 (> window_end 10:10 + watermark 2min) → forces window to close
      val sentinel = Timestamp.valueOf("2022-10-01 10:13:00")
      memStream.addData(Row("Sentinel", "99-999-0001", "99-999", sentinel))

      query.processAllAvailable()

      val results = spark.table("windowed_test_closed")
      val alice = results
        .filter(col("author") === "Alice" && col("region") === "14-101")
        .collect()

      assert(alice.length == 1, s"expected 1 Alice row, got ${alice.length}")
      assert(alice(0).getAs[Long]("monuments_pictured") == 2L)

      query.stop()
    }

    test("excludes late rows older than the watermark") {
      implicit val sqlContext: SQLContext = spark.sqlContext
      import org.apache.spark.sql.execution.streaming.MemoryStream

      val memStream = MemoryStream[Row]
      val windowed = Queries.windowedAgg(memStream.toDF(), "10 minutes", "2 minutes")

      val query = windowed.writeStream
        .format("memory")
        .queryName("windowed_test_late")
        .outputMode("append")
        .start()

      // Advance watermark to 10:23 by injecting a row at 10:25
      val advance = Timestamp.valueOf("2022-10-01 10:25:00")
      memStream.addData(Row("Bob", "14-102-0001", "14-102", advance))
      // Sentinel to close Bob's window
      val sentinel = Timestamp.valueOf("2022-10-01 10:38:00")
      memStream.addData(Row("Sentinel", "99-999-0001", "99-999", sentinel))
      query.processAllAvailable()

      // Late row: timestamp 10:00 is older than current watermark 10:23 → dropped
      val late = Timestamp.valueOf("2022-10-01 10:00:00")
      memStream.addData(Row("Alice", "14-101-0001", "14-101", late))
      query.processAllAvailable()

      val results = spark.table("windowed_test_late")
      val alice = results.filter(col("author") === "Alice").collect()
      assert(alice.isEmpty, s"late row should be excluded but found: ${alice.mkString}")

      query.stop()
    }
  }
  ```

- [ ] **Step 2: Run tests — they should pass immediately**

  `Queries.scala` was created in Task 3 so there is no failing-first cycle here. The TDD for `Queries.windowedAgg` was completed incrementally: the streaming behaviour (watermark, window closure) is genuinely new logic that is being verified for the first time in this task.

  Run: `sbt "spark-streaming/testOnly *WindowedQuerySpec"`

  Expected: `2 tests, 0 failures`

  If `processAllAvailable()` hangs: the watermark is not advancing — verify that the sentinel row timestamp is strictly greater than `window_end + watermarkDuration`.

- [ ] **Step 4: Run all tests so far**

  Run: `sbt "spark-streaming/test"`

  Expected: `9 tests, 0 failures` (5 from TransformationsSpec + 2 from CumulativeQuerySpec + 2 from WindowedQuerySpec)

- [ ] **Step 5: Commit**

  ```bash
  git add spark-streaming/src/test/scala/org/scalawiki/spark/WindowedQuerySpec.scala
  git commit -m "test: add WindowedQuerySpec with MemoryStream and watermark sentinel"
  ```

---

## Task 5: `WlmStreamingApp` Main Class

**Files:**
- Create: `spark-streaming/src/main/resources/application.conf`
- Modify: `spark-streaming/src/main/scala/org/scalawiki/spark/WlmStreamingApp.scala` (replace placeholder)

- [ ] **Step 1: Create `application.conf`**

  Create `spark-streaming/src/main/resources/application.conf`:

  ```hocon
  spark-streaming {
    input-dir       = "input"
    output-dir      = "output"
    checkpoint-dir  = "checkpoints"
    simulator-interval-ms = 5000
    window-duration = "10 minutes"
    watermark-duration = "2 minutes"
  }
  ```

- [ ] **Step 2: Replace the placeholder `WlmStreamingApp.scala`**

  Overwrite `spark-streaming/src/main/scala/org/scalawiki/spark/WlmStreamingApp.scala`:

  ```scala
  package org.scalawiki.spark

  import com.typesafe.config.ConfigFactory
  import org.apache.spark.sql.DataFrame
  import org.apache.spark.sql.SparkSession

  object WlmStreamingApp {

    def main(args: Array[String]): Unit = {
      val cfg         = ConfigFactory.load().getConfig("spark-streaming")
      val inputDir    = cfg.getString("input-dir")
      val outputDir   = cfg.getString("output-dir")
      val checkpointDir = cfg.getString("checkpoint-dir")
      val windowDur   = cfg.getString("window-duration")
      val watermarkDur = cfg.getString("watermark-duration")

      val spark = SparkSession.builder()
        .appName("WlmStreamingApp")
        .master("local[*]")
        .getOrCreate()

      spark.sparkContext.setLogLevel("WARN")

      val rawStream = spark.readStream
        .schema(WlmSchema.csvSchema)
        .option("header", "true")
        .csv(inputDir)

      val transformed = Transformations.transform(rawStream)

      // Query 1: cumulative (complete mode) — foreachBatch writes console + Parquet
      val q1 = Queries.cumulativeAgg(transformed)
        .writeStream
        .outputMode("complete")
        .option("checkpointLocation", s"$checkpointDir/cumulative")
        .foreachBatch { (batchDf: DataFrame, _: Long) =>
          batchDf.show(truncate = false)
          batchDf.write.mode("overwrite").parquet(s"$outputDir/cumulative")
        }
        .start()

      // Query 2: windowed (append mode + watermark) — foreachBatch writes console + Parquet
      val q2 = Queries.windowedAgg(transformed, windowDur, watermarkDur)
        .writeStream
        .outputMode("append")
        .option("checkpointLocation", s"$checkpointDir/windowed")
        .foreachBatch { (batchDf: DataFrame, _: Long) =>
          batchDf.show(truncate = false)
          batchDf.write.mode("append").parquet(s"$outputDir/windowed")
        }
        .start()

      spark.streams.awaitAnyTermination()
    }
  }
  ```

- [ ] **Step 3: Verify the module still compiles**

  Run: `sbt "spark-streaming/compile"`

  Expected: `[success]`

- [ ] **Step 4: Commit**

  ```bash
  git add spark-streaming/src/main/
  git commit -m "feat: implement WlmStreamingApp with two foreachBatch streaming queries"
  ```

---

## Task 6: `ImageUploadSimulator`

**Files:**
- Create: `spark-streaming/src/main/scala/org/scalawiki/spark/ImageUploadSimulator.scala`
- Create: `spark-streaming/src/test/scala/org/scalawiki/spark/ImageUploadSimulatorSpec.scala`

`ImageUploadSimulator` accepts a `java.nio.file.FileSystem` so tests can inject a jimfs in-memory filesystem instead of touching disk. Production uses `FileSystems.getDefault`.

- [ ] **Step 1: Write the failing test in `ImageUploadSimulatorSpec.scala`**

  Create `spark-streaming/src/test/scala/org/scalawiki/spark/ImageUploadSimulatorSpec.scala`:

  ```scala
  package org.scalawiki.spark

  import com.google.common.jimfs.{Configuration, Jimfs}
  import org.scalatest.funsuite.AnyFunSuite

  import java.nio.file.{Files, FileSystem}

  class ImageUploadSimulatorSpec extends AnyFunSuite {

    private def makeFs(): FileSystem = Jimfs.newFileSystem(Configuration.unix())

    test("copies source files into target directory") {
      val fs = makeFs()
      val srcDir = fs.getPath("/src")
      Files.createDirectories(srcDir)
      val f1 = srcDir.resolve("a.csv")
      val f2 = srcDir.resolve("b.csv")
      Files.write(f1, "col1,col2\nv1,v2".getBytes)
      Files.write(f2, "col1,col2\nv3,v4".getBytes)

      val targetDir = fs.getPath("/target")
      val sim = new ImageUploadSimulator(
        sourcePaths  = Seq("/src/a.csv", "/src/b.csv"),
        targetDirStr = "/target",
        intervalMs   = 0L,
        fs           = fs
      )
      sim.run()

      assert(Files.exists(targetDir.resolve("a.csv")))
      assert(Files.exists(targetDir.resolve("b.csv")))
      assert(new String(Files.readAllBytes(targetDir.resolve("a.csv"))) == "col1,col2\nv1,v2")
    }

    test("creates target directory if it does not exist") {
      val fs = makeFs()
      val src = fs.getPath("/src/c.csv")
      Files.createDirectories(src.getParent)
      Files.write(src, "data".getBytes)

      val sim = new ImageUploadSimulator(
        sourcePaths  = Seq("/src/c.csv"),
        targetDirStr = "/new/target",
        intervalMs   = 0L,
        fs           = fs
      )
      sim.run()
      assert(Files.exists(fs.getPath("/new/target/c.csv")))
    }

    test("copies files one by one with the configured interval") {
      val fs = makeFs()
      val srcDir = fs.getPath("/src")
      Files.createDirectories(srcDir)
      val files = (1 to 3).map { i =>
        val p = srcDir.resolve(s"f$i.csv")
        Files.write(p, s"data$i".getBytes)
        p
      }

      val copiedAt = scala.collection.mutable.ArrayBuffer[Long]()
      val targetDir = fs.getPath("/target")

      // Subclass to record timestamps
      val sim = new ImageUploadSimulator(
        sourcePaths  = files.map(_.toString),
        targetDirStr = "/target",
        intervalMs   = 100L,
        fs           = fs
      ) {
        override protected def afterCopy(): Unit = copiedAt += System.currentTimeMillis()
      }
      sim.run()

      assert(copiedAt.length == 3)
      // Each copy should be ~100ms apart; allow generous margin for CI
      val gaps = copiedAt.sliding(2).map { case Seq(a, b) => b - a }.toSeq
      gaps.foreach(gap => assert(gap >= 80L, s"gap $gap ms too short"))
    }
  }
  ```

- [ ] **Step 2: Run tests to confirm they fail**

  Run: `sbt "spark-streaming/testOnly *ImageUploadSimulatorSpec"`

  Expected: compilation error — `ImageUploadSimulator` not found.

- [ ] **Step 3: Create `ImageUploadSimulator.scala`**

  Create `spark-streaming/src/main/scala/org/scalawiki/spark/ImageUploadSimulator.scala`:

  ```scala
  package org.scalawiki.spark

  import java.nio.file.{FileSystem, FileSystems, Files, StandardCopyOption}

  /**
   * Copies source CSV files one-by-one into the target directory at a fixed interval,
   * simulating a stream of WLM image upload files arriving over time.
   *
   * @param sourcePaths  string paths to source CSV files, resolved via `fs`
   * @param targetDirStr string path for the destination (watched input) directory, resolved via `fs`
   * @param intervalMs   milliseconds to sleep between copies
   * @param fs           filesystem to use; default is the JVM default filesystem
   */
  class ImageUploadSimulator(
    sourcePaths:  Seq[String],
    targetDirStr: String,
    intervalMs:   Long,
    fs:           FileSystem = FileSystems.getDefault
  ) {

    def run(): Unit = {
      val targetDir = fs.getPath(targetDirStr)
      Files.createDirectories(targetDir)
      sourcePaths.foreach { srcStr =>
        val src  = fs.getPath(srcStr)
        val dest = targetDir.resolve(src.getFileName.toString)
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING)
        afterCopy()
        Thread.sleep(intervalMs)
      }
    }

    /** Hook called after each file is copied. Override in tests to record timing. */
    protected def afterCopy(): Unit = ()
  }

  object ImageUploadSimulator {

    def main(args: Array[String]): Unit = {
      import com.typesafe.config.ConfigFactory

      if (args.isEmpty) {
        System.err.println("Usage: ImageUploadSimulator <csv-file1> [csv-file2 ...]")
        sys.exit(1)
      }

      val cfg       = ConfigFactory.load().getConfig("spark-streaming")
      val targetDir = cfg.getString("input-dir")
      val interval  = cfg.getLong("simulator-interval-ms")

      new ImageUploadSimulator(
        sourcePaths  = args.toSeq,
        targetDirStr = targetDir,
        intervalMs   = interval
      ).run()
    }
  }
  ```

- [ ] **Step 4: Run tests and verify they pass**

  Run: `sbt "spark-streaming/testOnly *ImageUploadSimulatorSpec"`

  Expected: `3 tests, 0 failures`

- [ ] **Step 5: Run the full test suite**

  Run: `sbt "spark-streaming/test"`

  Expected: `12 tests, 0 failures` (5 + 2 + 2 + 3)

- [ ] **Step 6: Commit**

  ```bash
  git add spark-streaming/src/
  git commit -m "feat: add ImageUploadSimulator with jimfs-based tests"
  ```

---

## Final Verification

- [ ] **Run the complete scalawiki test suite to check no regressions**

  Run: `sbt test`

  Expected: all existing tests pass; `spark-streaming` tests also green.

- [ ] **Verify assembly fat JAR builds**

  Run: `sbt "spark-streaming/assembly"`

  Expected: `[success]` and `spark-streaming/target/scala-2.13/spark-streaming-0.7.0-SNAPSHOT.jar` created.
