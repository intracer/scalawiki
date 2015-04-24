package org.scalawiki.xml

import java.io.{OutputStream, Writer}
import javax.xml.stream.{XMLOutputFactory, XMLStreamWriter}

import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter
import org.codehaus.stax2.{XMLOutputFactory2, XMLStreamWriter2}
import org.scalawiki.Timestamp
import org.scalawiki.dto.{IpContributor, User, Page, Revision}
import org.scalawiki.xml.XmlWriter._

class XmlWriter(writer: XMLStreamWriter) {

  def write(pages: Seq[Page]) = {

    writer.writeStartDocument("utf-8", "1.0")

    writer.setPrefix("xml", XML_NS)

    writer.writeStartElement("mediawiki")

    for ((k, v) <- mwAttrs)
      writer.writeAttribute(k, v)

    pages.foreach(writePage)

    //    writer.writeEndElement()

    writer.writeEndDocument()

    writer.flush()
    writer match {
      case stax2Writer: XMLStreamWriter2 => stax2Writer.closeCompletely()
      case _ => writer.close()
    }
  }

  def writePage(page: Page) = {

    writer.writeStartElement("page")

    writeElement("title", page.title)
    writeElement("ns", page.ns)
    writeElement("id", page.id.get)

    page.revisions.foreach(writeRevision)

    writer.writeEndElement()
  }

  def writeRevision(rev: Revision) = {
    writer.writeStartElement("revision")

    writeElement("id", rev.id.get)

    rev.parentId.foreach(writeElement("parentid", _))

    writeElement("timestamp", rev.timestamp.map(Timestamp.format).getOrElse(""))

    for (contributor <- rev.user) {
      writer.writeStartElement("contributor")
      contributor match {
        case user: User =>
          user.login.foreach(writeElement("username", _))
          user.id.foreach(writeElement("id", _))
        case ip: IpContributor =>
          writeElement("ip", ip.ip)
      }

      writer.writeEndElement()
    }

    //    if (rev.minor.exists(identity))
    //      writeEmptyElement("minor")

    rev.comment.foreach(writeElement("comment", _))

    writeElement("model", "wikitext")
    writeElement("format", "text/x-wiki")

    writeElement("text", rev.content.getOrElse(""), Seq("xml:space" -> "preserve"))
    rev.sha1.foreach(writeElement("sha1", _))

    writer.writeEndElement()
  }

  def writeEmptyElement(name: String) = {
    writer.writeEmptyElement(name)
  }

  def writeElement(name: String, value: Any, attrs: Seq[(String, String)] = Seq.empty) = {
    writer.writeStartElement(name)
    for ((k, v) <- attrs)
      writer.writeAttribute(k, v)

    writer.writeCharacters(value.toString)
    writer.writeEndElement()
  }

}

object XmlWriter {

  val outputFactory = XMLOutputFactory.newFactory().asInstanceOf[XMLOutputFactory2]
  outputFactory.configureForSpeed()

  def create(w: Writer) = {
    val writer = new IndentingXMLStreamWriter(XmlWriter.outputFactory.createXMLStreamWriter(w))
    new XmlWriter(writer)
  }

  def create(os: OutputStream) = {
    val writer = new IndentingXMLStreamWriter(XmlWriter.outputFactory.createXMLStreamWriter(os))
    new XmlWriter(writer)
  }


  //  def createWriter(): XMLStreamWriter =
  //    xmlOutputFactory.createXMLStreamWriter(System.out)

  val XML_NS = "http://www.w3.org/XML/1998/namespace"
  val version: String = "0.10"
  val ns: String = "http://www.mediawiki.org/xml/export-" + version + "/"
  val schema: String = "http://www.mediawiki.org/xml/export-" + version + ".xsd"

  // xsi:schemaLocation="http://www.mediawiki.org/xml/export-0.10/ http://www.mediawiki.org/xml/export-0.10.xsd" version="0.10" xml:lang="uk" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.mediawiki.org/xml/export-0.10/"

  val mwAttrs = Seq[(String, String)](
    "xmlns" -> ns,
    "xmlns:xsi" -> "http://www.w3.org/2001/XMLSchema-instance",
    "xsi:schemaLocation" -> (ns + " " + schema),
    "version" -> version,
    "xml:lang" -> "en" //TODO lang
  )


}