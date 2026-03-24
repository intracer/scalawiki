package org.scalawiki.wlx

import com.github.tototoshi.csv.CSVWriter
import org.scalawiki.dto.Image

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ImageCsvExporter {

  val columns: Seq[String] = Seq(
    "title", "author", "upload_date", "monument_id", "page_id",
    "width", "height", "size_bytes", "mime", "camera", "exif_date",
    "categories", "special_nominations", "url", "page_url"
  )

  def imageToRow(image: Image): Map[String, String] = Map(
    "title"               -> image.title,
    "author"              -> image.author.getOrElse(""),
    "upload_date"         -> image.date.map(_.toString).getOrElse(""),
    "monument_id"         -> image.monumentIds.mkString(";"),
    "page_id"             -> image.pageId.map(_.toString).getOrElse(""),
    "width"               -> image.width.map(_.toString).getOrElse(""),
    "height"              -> image.height.map(_.toString).getOrElse(""),
    "size_bytes"          -> image.size.map(_.toString).getOrElse(""),
    "mime"                -> image.mime.getOrElse(""),
    "camera"              -> image.metadata.flatMap(_.camera).getOrElse(""),
    "exif_date"           -> image.metadata.flatMap(_.date).map(_.toString).getOrElse(""),
    "categories"          -> image.categories.mkString(";"),
    "special_nominations" -> image.specialNominations.mkString(";"),
    "url"                 -> image.url.getOrElse(""),
    "page_url"            -> image.pageUrl.getOrElse("")
  )

  def filename(
      campaign: String,
      contestYear: Int,
      isCurrent: Boolean,
      outputDir: String
  ): String = {
    val name = if (isCurrent) {
      val fmt = DateTimeFormatter.ofPattern("MM-dd-HHmm")
      s"$campaign-$contestYear-${LocalDateTime.now().format(fmt)}.csv"
    } else {
      s"$campaign-$contestYear-images.csv"
    }
    if (outputDir.nonEmpty) s"$outputDir${java.io.File.separator}$name" else name
  }

  def export(
      imageDb: ImageDB,
      campaign: String,
      isCurrent: Boolean,
      outputDir: String
  ): Unit = {
    val images = imageDb.images.toSeq
    if (images.isEmpty) return

    val path = filename(campaign, imageDb.contest.year, isCurrent, outputDir)
    val writer = CSVWriter.open(new File(path), "UTF-8")
    try {
      writer.writeRow(columns)
      images.foreach { image =>
        val row = imageToRow(image)
        writer.writeRow(columns.map(c => row.getOrElse(c, "")))
      }
    } finally {
      writer.close()
    }
  }
}
