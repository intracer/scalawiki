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
