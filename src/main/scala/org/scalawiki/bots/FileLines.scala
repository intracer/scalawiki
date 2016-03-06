package org.scalawiki.bots

import java.nio.file.{Files, Paths}

import scala.io.Source

object FileLines {

  val nl = System.lineSeparator

  def read(filename: String): Seq[String] =
     Source.fromFile(filename).getLines.toBuffer

  def write(filename: String, lines: Seq[String]) =
    Files.write(Paths.get(filename), lines.mkString(nl).getBytes)

}
