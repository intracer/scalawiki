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
