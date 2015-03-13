package org.scalawiki.xml

import java.io.{Reader, InputStream, StringReader, File}
import javax.xml.stream.{XMLStreamReader, XMLStreamConstants, XMLInputFactory}

import org.codehaus.stax2.{XMLStreamReader2, XMLInputFactory2}
import org.scalawiki.Timestamp
import org.scalawiki.dto.{Page, Revision}

import scala.collection.{Iterator, AbstractIterator}

case class SiteInfo(name: Option[String], db: Option[String], generator: Option[String])

class XmlParser(val parser: XMLStreamReader) extends Iterable[Page] {

  private var _siteInfo: Option[SiteInfo] = None
  private var _namespaces: Map[Int, String] = Map.empty
  private var parsedSiteInfo: Boolean = false

  override def iterator = {

    readSiteInfo()

    new AbstractIterator[Page] {

      override def hasNext = findElementStart("page")

      override def next() = readPage().getOrElse(Iterator.empty.next())

    }
  }

  def siteInfo = {
    readSiteInfo()
    _siteInfo
  }

  def namespaces = {
    readSiteInfo()
    _namespaces
  }

  def close() =
    parser match {
      case stax2Parser: XMLStreamReader2 => stax2Parser.closeCompletely()
      case _ => parser.close()
    }

  private def readSiteInfo() = {
    if (!parsedSiteInfo) {
      if (findElementStart("siteinfo", "page")) {
        val sitename = readElement("sitename")
        val dbname = readElement("dbname")
        val generator = readElement("generator")

        _siteInfo = Some(SiteInfo(sitename, dbname, generator))
        readNamespaces()
      }
      parsedSiteInfo = true
    }
  }

  private def readNamespaces(): Unit = {
    if (findElementStart("namespaces")) {

      while (findElementStart("namespace", parent = "namespaces")) {

        val key = parser.getAttributeValue("", "key").toInt
        val prefix = parser.getElementText

        _namespaces += (key -> prefix)
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
        val parentId = readElement("parentid", "timestamp").map(_.toInt)
        val timestamp = readElement("timestamp").map(Timestamp.parse)

        findElementStart("contributor")
        val user = readElement("username", "ip", "contributor")
        val userId = readElement("id", "ip", "contributor").map(_.toInt)
        val userIp = readElement("ip", parent = "contributor")

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

  private def readElement(name: String, next: String = "", parent: String = ""): Option[String] = {
    if (findElementStart(name, next, parent))
      Some(parser.getElementText)
    else
      None
  }

  private def findElementStart(name: String, next: String = "", parent: String = ""): Boolean = {
    val event = parser.getEventType
    event match {
      case XMLStreamConstants.START_ELEMENT if parser.getLocalName == name => true
      case XMLStreamConstants.START_ELEMENT if parser.getLocalName == next => false
      case XMLStreamConstants.END_ELEMENT if parser.getLocalName == parent => false
      case _ =>
        while (parser.hasNext) {
          val event = parser.next()
          event match {
            case XMLStreamConstants.START_ELEMENT if parser.getLocalName == name => return true
            case XMLStreamConstants.START_ELEMENT if parser.getLocalName == next => return false
            case XMLStreamConstants.END_ELEMENT if parser.getLocalName == parent => return false
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

  def parseFile(filename: String) =
    new XmlParser(xmlInputFactory.createXMLStreamReader(new File(filename)))

  //  def parseUrl(url: URL) = new XmlParser(xmlInputFactory.createXMLStreamReader(url))

  def parseInputStream(is: InputStream) = new XmlParser(xmlInputFactory.createXMLStreamReader(is))

  def parseReader(reader: Reader) = new XmlParser(xmlInputFactory.createXMLStreamReader(reader))

  def parseString(data: String) = parseReader(new StringReader(data))

  def newXmlInputFactory: XMLInputFactory2 = {
    val xmlInputFactory = XMLInputFactory.newInstance().asInstanceOf[XMLInputFactory2]
    xmlInputFactory.configureForSpeed()
    xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)
    xmlInputFactory
  }
}