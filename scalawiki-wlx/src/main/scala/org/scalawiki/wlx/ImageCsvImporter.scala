package org.scalawiki.wlx

import com.github.tototoshi.csv.CSVReader
import org.scalawiki.dto.{Image, ImageMetadata}

import java.io.File
import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

object ImageCsvImporter {

  private val exifPattern = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

  private def optStr(s: String): Option[String] = if (s.isEmpty) None else Some(s)

  private def optLong(s: String): Option[Long] = if (s.isEmpty) None else Some(s.toLong)

  private def optInt(s: String): Option[Int] = if (s.isEmpty) None else Some(s.toInt)

  private def splitSeq(s: String): Seq[String] = if (s.isEmpty) Seq.empty else s.split(";").toSeq

  private def splitSet(s: String): Set[String] = splitSeq(s).toSet

  private def toExifRaw(exifDate: String): String =
    ZonedDateTime
      .parse(exifDate)
      .withZoneSameInstant(ZoneOffset.UTC)
      .format(exifPattern)

  def rowToImage(row: Map[String, String]): Image = {
    val camera = optStr(row.getOrElse("camera", ""))
    val exifDate = optStr(row.getOrElse("exif_date", ""))
    val metadata = (camera, exifDate) match {
      case (None, None) => None
      case _ =>
        val data = camera.map("Model" -> _).toMap ++
          exifDate.map(d => "DateTimeOriginal" -> toExifRaw(d)).toMap
        Some(ImageMetadata(data))
    }

    Image(
      title = row.getOrElse("title", ""),
      url = optStr(row.getOrElse("url", "")),
      pageUrl = optStr(row.getOrElse("page_url", "")),
      size = optLong(row.getOrElse("size_bytes", "")),
      width = optInt(row.getOrElse("width", "")),
      height = optInt(row.getOrElse("height", "")),
      author = optStr(row.getOrElse("author", "")),
      date = optStr(row.getOrElse("upload_date", "")).map(ZonedDateTime.parse),
      monumentIds = splitSeq(row.getOrElse("monument_id", "")),
      pageId = optLong(row.getOrElse("page_id", "")),
      metadata = metadata,
      categories = splitSet(row.getOrElse("categories", "")),
      specialNominations = splitSet(row.getOrElse("special_nominations", "")),
      mime = optStr(row.getOrElse("mime", ""))
    )
  }

  def imagesFromCsv(path: String): Seq[Image] = {
    val file = new File(path)
    if (!file.exists()) return Seq.empty

    val reader = CSVReader.open(file, "UTF-8")
    try {
      reader.allWithHeaders().map(rowToImage)
    } finally {
      reader.close()
    }
  }
}
