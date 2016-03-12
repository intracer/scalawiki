package org.scalawiki.bots

import java.nio.file.{Files, Paths}

import scala.io.{Codec, Source}

/**
  * Save and load lines to/from file
  */
object FileLines {

  val nl = System.lineSeparator

  /**
    * Read lines from file
    * @param filename file to read
    * @param codec character encoding/decoding preferences, default is [[scala.io.Codec.defaultCharsetCodec()]]
    * @return lines from the file
    */
  def read(filename: String)(implicit codec: Codec): Seq[String] =
    Source.fromFile(filename).getLines.toBuffer

  /**
    * Save lines to file, overwriting it
    *
    * @param filename file to write to
    * @param lines    lines to save
    * @param codec character encoding/decoding preferences, default is [[scala.io.Codec.defaultCharsetCodec()]]
    * @return
    */
  def write(filename: String, lines: Seq[String])(implicit codec: Codec) =
    Files.write(Paths.get(filename), lines.mkString(nl).getBytes(codec.charSet))

}
