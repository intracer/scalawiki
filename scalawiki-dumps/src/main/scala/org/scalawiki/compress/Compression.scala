package org.scalawiki.compress

import java.io.{InputStream, OutputStream}
import java.nio.file.{Files, Path}

import org.apache.commons.compress.compressors.bzip2.{
  BZip2CompressorInputStream,
  BZip2CompressorOutputStream
}
import org.apache.commons.compress.compressors.gzip.{
  GzipCompressorInputStream,
  GzipCompressorOutputStream
}

abstract class Compression(val ext: String) {
  def inputStream(in: InputStream): InputStream

  def outputStream(out: OutputStream): OutputStream
}

object Bz2 extends Compression("bz2") {
  override def inputStream(in: InputStream) = new BZip2CompressorInputStream(in)

  override def outputStream(out: OutputStream) =
    new BZip2CompressorOutputStream(out)
}

object Gz extends Compression("gz") {
  override def inputStream(in: InputStream) = new GzipCompressorInputStream(in)

  override def outputStream(out: OutputStream) = new GzipCompressorOutputStream(
    out
  )
}

object SevenZ extends Compression("7z") {
  override def inputStream(in: InputStream) = ???

  override def outputStream(out: OutputStream) = ???
}

object NoCompression extends Compression("") {
  override def inputStream(in: InputStream) = in

  override def outputStream(out: OutputStream) = out
}

object Compression {

  val compressions = Seq(Bz2, Gz, SevenZ, NoCompression)

  def get(path: Path): Compression = {
    val ext = path.getFileName.toString.split("\\.").last

    compressions.find(_.ext == ext).getOrElse(NoCompression)
  }

  def inputStream(path: Path) =
    get(path).inputStream(Files.newInputStream(path))

  def outputStream(path: Path) =
    get(path).outputStream(Files.newOutputStream(path))

}
