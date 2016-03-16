package org.scalawiki.xml

import java.nio.file.{FileSystem, Paths, Files, Path}

import com.google.common.jimfs.{Configuration, Jimfs}
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach

class DataDumpSpec extends Specification with BeforeEach {

  val filename = "ukwiki-20150311-pages-articles-multistream.xml.bz2"

  override def before = {

  }

  "data dump" should {
    "fill data from filesystem" in {
      val fs = Jimfs.newFileSystem(Configuration.unix())
      val path = "/path/to"
      val fullname =  path + "/" + filename

      val dd = new DumpFileInfo(fs.getPath(fullname))

      dd.filename === filename
      dd.directory.toString === path
      dd.database === Some("ukwiki")
      dd.date === "20150311"
      dd.isPageIndex === false
      dd.isPagesXml === true
    }

    "find bz2 index" in {
      val dir = newInMemoryDirectory()
      val dumpPath = createFile(dir, filename)

      val index = "ukwiki-20150311-pages-articles-multistream-index.txt.bz2"
      val indexPath = createFile(dir, index)

      val dd = DumpFile.get(dumpPath).get.asInstanceOf[PagesDump]

      dd.findIndexFile.map(_.toString) ===  Some(indexPath.toString)
    }

    "find uncompressed index" in {
      val dir = newInMemoryDirectory()
      val dumpPath = createFile(dir, filename)

      val index = "ukwiki-20150311-pages-articles-multistream-index.txt"
      val indexPath = createFile(dir, index)

      val dd = DumpFile.get(dumpPath).get.asInstanceOf[PagesDump]

      dd.findIndexFile.map(_.toString) ===  Some(indexPath.toString)
    }

    "do not find index with wrong date" in {
      val dir = newInMemoryDirectory()
      val dumpPath = createFile(dir, filename)

      val index = "ukwiki-20140915-pages-articles-multistream-index.txt.bz2"
      val indexPath = createFile(dir, index)

      val dd = DumpFile.get(dumpPath).get.asInstanceOf[PagesDump]

      dd.findIndexFile === None
    }

  }

  def newInMemoryDirectory(path: String = "/foo", configuration: Configuration = Configuration.unix()): Path = {
    val fs = Jimfs.newFileSystem(configuration)
    val dir = fs.getPath(path)
    Files.createDirectory(dir)
  }

  def createFile(directory: Path, filename: String): Path =
    Files.createFile(directory.resolve(filename))

}
