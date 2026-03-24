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
   *      Note: this yields sub-region granularity (e.g. "14-101" from "14-101-0001"),
   *      not the oblast-level code ("14") used by Monument.getRegionId in scalawiki-wlx.
   *      This is intentional — the streaming module is self-contained and aggregates at
   *      sub-region level.
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
