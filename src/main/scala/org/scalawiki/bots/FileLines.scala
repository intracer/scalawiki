package org.scalawiki.bots

import java.nio.file.{Files, Paths}

import scala.io.Source

/**
  * Save and load lines to/from file
  */
object FileLines {

  val nl = System.lineSeparator

  /**
    * Read lines from file
    * @param filename file to read
    * @return lines from the file
    */
  def read(filename: String): Seq[String] =
    Source.fromFile(filename).getLines.toBuffer

  /**
    * Save lines to file, overwriting it
    * @param filename file to write to
    * @param lines lines to save
    * @return
    */
  def write(filename: String, lines: Seq[String]) =
    Files.write(Paths.get(filename), lines.mkString(nl).getBytes)

}
