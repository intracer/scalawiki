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
      .sort(col("monuments_pictured").desc)

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
