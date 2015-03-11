package org.scalawiki.xml

import java.io.{StringReader, File}
import javax.xml.stream.{XMLStreamReader, XMLStreamConstants, XMLInputFactory}

import org.codehaus.stax2.{XMLStreamReader2, XMLInputFactory2}
import org.scalawiki.Timestamp
import org.scalawiki.dto.{Page, Revision}

import scala.collection.{Iterator, AbstractIterator}

case class SiteInfo(name: Option[String], db: Option[String], generator: Option[String])

class XmlParser(val parser: XMLStreamReader) extends Iterable[Page] {

  var siteInfo: Option[SiteInfo] = None
  var namespaces: Map[Int, String] = Map.empty

  override def iterator = {

    readSiteInfo()

    new AbstractIterator[Page] {

      override def hasNext = findElementStart("page")

      override def next() = readPage().getOrElse(Iterator.empty.next())

    }
  }

  def close() =
    parser match {
      case stax2Parser: XMLStreamReader2 => stax2Parser.closeCompletely()
      case _ => parser.close()
    }

  private def readSiteInfo() = {
    if (findElementStart("siteinfo", "page")) {
      val sitename = readElement("sitename")
      val dbname = readElement("dbname")
      val generator = readElement("generator")

      siteInfo = Some(SiteInfo(sitename, dbname, generator))
      readNamespaces()
    }
  }

  private def readNamespaces(): Unit = {
    if (findElementStart("namespaces")) {

      while (findElementStart("namespace", parent = "namespaces")) {

        val key = parser.getAttributeValue("", "key").toInt
        val prefix = parser.getElementText

        namespaces += (key -> prefix)
      }
      findElementEnd("namespaces")
    }
  }

  private def readPage(): Option[Page] =
    if (findElementStart("page"))
      for (title <- readElement("title");
           ns <- readElement("ns").map(_.toInt);
           id <- readElement("id").map(_.toInt)
      ) yield {
        val revisions = readRevisions()
        new Page(id, ns, title, revisions.toSeq)
      }
    else
      None


  private def readRevisions(): Seq[Revision] = {
    // TODO streaming and filtering
    var revisions = Seq.empty[Revision]
    while (findElementStart("revision", parent = "page")) {

      readElement("id").map(_.toInt).foreach { id =>
        val parentId = readElement("parentid").map(_.toInt)
        val timestamp = readElement("timestamp").map(Timestamp.parse)

        findElementStart("contributor")
        val user = readElement("username")
        val userId = readElement("id").map(_.toInt)

        val comment = readElement("comment")
        val text = readElement("text")
        val sha1 = readElement("sha1")

        findElementEnd("revision")

        revisions :+=
          Revision(
            revId = id,
            parentId = parentId,
            user = user,
            userId = userId,
            timestamp = timestamp,
            comment = comment,
            content = text,
            sha1 = sha1
          )
      }
    }
    revisions
  }

  private def readElement(name: String): Option[String] = {
    while (parser.next() != XMLStreamConstants.START_ELEMENT ||
      parser.getLocalName != name) {}

    if (parser.getLocalName == name)
      Some(parser.getElementText)
    else
      None
  }

  private def findElementStart(name: String, next: String = "", parent: String = ""): Boolean = {
    if (parser.getEventType == XMLStreamConstants.START_ELEMENT &&
      parser.getLocalName == name) {
      true
    } else {
      while (parser.hasNext) {
        val event = parser.next()
        event match {
          case XMLStreamConstants.START_ELEMENT
            if parser.getLocalName == name =>
            return true
          case XMLStreamConstants.START_ELEMENT
            if parser.getLocalName == next =>
            return false
          case XMLStreamConstants.END_ELEMENT
            if parser.getLocalName == parent =>
            return false
          case _ =>
        }
      }
      false
    }
  }

  private def findElementEnd(name: String) = {
    if (parser.getEventType != XMLStreamConstants.END_ELEMENT || parser.getLocalName != name)
      while (parser.next() != XMLStreamConstants.END_ELEMENT || parser.getLocalName != name) {}
  }

}

object XmlParser {

  val xmlInputFactory = newXmlInputFactory

  def parseFile(filename: String) = {
    val file = new File(filename)
    val xmlReader = xmlInputFactory.createXMLStreamReader(file)
    new XmlParser(xmlReader)
  }

  def parseString(data: String) = {
    val reader = new StringReader(data)
    val xmlReader = xmlInputFactory.createXMLStreamReader(reader)
    new XmlParser(xmlReader)
  }


  def newXmlInputFactory: XMLInputFactory2 = {
    val xmlInputFactory = XMLInputFactory.newInstance().asInstanceOf[XMLInputFactory2]
    xmlInputFactory.configureForSpeed()
    xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)
    xmlInputFactory
  }
}