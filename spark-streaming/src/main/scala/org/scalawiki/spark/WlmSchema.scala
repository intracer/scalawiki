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
    StructField("author",         StringType,    nullable = true),
    StructField("monument",       StringType,    nullable = false), // non-null: produced by explode
    StructField("region",         StringType,    nullable = false), // non-null: regexp_replace on non-null monument
    StructField("upload_date_ts", TimestampType, nullable = true)
  ))
}
