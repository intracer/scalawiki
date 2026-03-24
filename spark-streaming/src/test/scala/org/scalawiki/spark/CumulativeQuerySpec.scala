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
