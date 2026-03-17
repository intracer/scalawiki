package org.scalawiki.wlx

import com.github.tototoshi.csv.CSVWriter

import java.io.File

object MonumentCsvExporter {

  /** Export monument rows to a CSV file.
    *
    * - Applies UaUkJsonMapping.applyMapping to each row.
    * - Computes header via UaUkJsonMapping.headerColumns (sql_data keys first, then alphabetical).
    * - Writes nothing if rows is empty.
    *
    * @param rows       raw monument parameter maps (as returned by parseToMaps)
    * @param mapping    loaded UaUkMapping (from UaUkJsonMapping.load)
    * @param outputPath path to the output CSV file
    */
  def export(
      rows: Iterable[Map[String, String]],
      mapping: UaUkMapping,
      outputPath: String
  ): Unit = {
    // Materialize once so we can iterate twice (header + rows)
    val mappedRows: Seq[Map[String, String]] =
      rows.map(UaUkJsonMapping.applyMapping(_, mapping)).toSeq

    if (mappedRows.isEmpty) return

    val headers = UaUkJsonMapping.headerColumns(mappedRows, mapping)

    val writer = CSVWriter.open(new File(outputPath))
    try {
      writer.writeRow(headers)
      mappedRows.foreach { row =>
        writer.writeRow(headers.map(h => row.getOrElse(h, "")))
      }
    } finally {
      writer.close()
    }
  }
}
