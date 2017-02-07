package org.scalawiki.xml

import java.nio.file.{Files, Path, Paths}

import scala.collection.JavaConverters._

abstract class DumpFile(info: DumpFileInfo)

class DumpFileInfo(val path: Path) {
  val filename = path.getFileName.toString
  val directory = path.getParent

  val dotParts = filename split "\\."

  val withoutExtension = dotParts(0)
  val extensions = dotParts slice(1, dotParts.length)

  val hyphensParts = withoutExtension split "-"
  val database: Option[String] = if (hyphensParts(0) == "pagecounts")
    None
  else
    Some(hyphensParts(0))

  val dumpType = if (hyphensParts(0) == "pagecounts")
    "pagecounts"
  else
    hyphensParts.slice(2, hyphensParts.length).mkString("-")

  val date = hyphensParts(1)

  def hasExtension(ext: String) =
    extensions exists (_ equalsIgnoreCase ext)

  def isXml = hasExtension("xml")

  def isTxt = hasExtension("txt")

  def isSql = hasExtension("sql")

  def isPagesXml = isXml && dumpType.startsWith("pages-")

  def isPageIndex = isTxt && dumpType.startsWith("pages-") && dumpType.endsWith("-index")

  def isPageCounts = dumpType == "pagecounts"

  def isIndex(path: Path) = path.getFileName.toString.startsWith(withoutExtension + "-index.txt")

  def dumpFile: Option[DumpFile] = {
    if (isPagesXml)
      Some(new PagesDump(this))
    else if (isPageIndex)
      Some(new PageIndexDump(this))
    else if (isPageCounts)
      Some(new PageViewCountDump(this))
    else if (isSql)
      Some(new SqlDump(this))
    else None
  }
}

class PagesDump(info: DumpFileInfo) extends DumpFile(info) {

  def findIndexFile: Option[Path] = {
    val paths = Files.newDirectoryStream(info.directory).asScala
    val indexFiles = paths.filter(info.isIndex)
    indexFiles.headOption
  }

}

class PageIndexDump(info: DumpFileInfo) extends DumpFile(info)

class SqlDump(info: DumpFileInfo) extends DumpFile(info)

class PageViewCountDump(info: DumpFileInfo) extends DumpFile(info)


object DumpFile {

  def get(path: Path): Option[DumpFile] = {
    val info = new DumpFileInfo(path)
    info.dumpFile
  }

  def process(filename: String): Unit = {
    val path = Paths.get(filename)
    val dump = get(path)

    println(dump)
  }

  def main(args: Array[String]) {
    val filename = args(0)
    process(filename)
  }
}
