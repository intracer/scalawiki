package client.compress

import java.io.{OutputStream, File, FileInputStream}

import org.apache.commons.compress.archivers.sevenz.{SevenZFile, SevenZOutputFile}

object SevenZ {

  val bufferSize: Int = 1 << 14


  def compress(sourceFilename: String, archiveFilename: String): Unit = {
    val sevenZFile = new File(archiveFilename)
    val sourceFile = new File(sourceFilename)
    val sevenZOutput = new SevenZOutputFile(sevenZFile)

    val entry = sevenZOutput.createArchiveEntry(sourceFile, sourceFilename)
    sevenZOutput.putArchiveEntry(entry)

    val is = new FileInputStream(sourceFile)
    val buff = new Array[Byte](bufferSize)
    var count = 0
    while ({count = is.read(buff); count > 0}) {
      sevenZOutput.write(buff, 0, count)
    }
    sevenZOutput.closeArchiveEntry()

    is.close()

    sevenZOutput.close()
  }

  def unCompress(archiveFilename: String, outputStream: OutputStream): Unit = {
    val sevenZFile = new SevenZFile(new File(archiveFilename))
    val entry = sevenZFile.getNextEntry

    if (entry != null) {
      var n: Int = 0
      var nread: Long = 0L

      val buff = new Array[Byte](bufferSize)
      while ( {n = sevenZFile.read(buff, 0, buff.length); n } > 0) {
        outputStream.write(buff, 0, n)
        nread += n
      }
    }
    sevenZFile.close()
  }


}
