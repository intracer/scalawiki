package org.scalawiki.xml

import java.io.{OutputStream, InputStream}

import scala.io.Source

class XmlIndex(val pages: Seq[PageIndex]) {

  val _byId = pages.groupBy(_.id).mapValues(_.head)
  val _byTitle = pages.groupBy(_.title).mapValues(_.head)
  val nl = System.lineSeparator()

  def save(os: OutputStream) = {
    pages.map(pi => (pi.toString + nl).getBytes).foreach(os.write)
  }

}

object XmlIndex {

  def fromParser(parser: XmlParser): Iterator[PageIndex] =
    parser.iterator.map { page =>
      PageIndex(parser.pageStartingByteOffset, page.id.get, page.title)
    }

  def fromInputStream(is: InputStream): Iterator[PageIndex] =
    Source.fromInputStream(is).getLines().map(PageIndex.fromString)
}