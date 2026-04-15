package org.scalawiki.spark

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SparkSession

object WlmStreamingApp {

  def main(args: Array[String]): Unit = {
    val cfg          = ConfigFactory.load().getConfig("spark-streaming")
    val inputDir     = cfg.getString("input-dir")
    val outputDir    = cfg.getString("output-dir")
    val checkpointDir = cfg.getString("checkpoint-dir")
    val windowDur    = cfg.getString("window-duration")
    val watermarkDur = cfg.getString("watermark-duration")

    val spark = SparkSession.builder()
      .appName("WlmStreamingApp")
      .master("local[*]")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    val rawStream = spark.readStream
      .schema(WlmSchema.csvSchema)
      .option("header", "true")
      .csv("images")

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
//    val q2 = Queries.windowedAgg(transformed, windowDur, watermarkDur)
//      .writeStream
//      .outputMode("append")
//      .option("checkpointLocation", s"$checkpointDir/windowed")
//      .foreachBatch { (batchDf: DataFrame, _: Long) =>
//        batchDf.show(truncate = false)
//        batchDf.write.mode("append").parquet(s"$outputDir/windowed")
//      }
//      .start()

    spark.streams.awaitAnyTermination()
  }
}
