package org.scalawiki.spark

import com.holdenkarau.spark.testing.StructuredStreamingBase
import org.apache.spark.sql.{Encoder, Row, SQLContext}
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder
import org.apache.spark.sql.functions.col
import org.scalatest.funsuite.AnyFunSuite

import java.sql.Timestamp

class WindowedQuerySpec extends AnyFunSuite with StructuredStreamingBase {

  private val schema = WlmSchema.transformedSchema
  implicit lazy val encoder: Encoder[Row] = ExpressionEncoder(schema)

  test("emits counts for closed windows") {
    import org.apache.spark.sql.execution.streaming.MemoryStream

    val memStream = MemoryStream[Row](1, spark.sqlContext)
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
    import org.apache.spark.sql.execution.streaming.MemoryStream

    val memStream = MemoryStream[Row](2, spark.sqlContext)
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
