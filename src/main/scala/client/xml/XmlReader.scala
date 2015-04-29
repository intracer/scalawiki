package client.xml

import java.io.RandomAccessFile
import java.nio.channels.Channels
import javax.xml.stream.{XMLInputFactory, XMLStreamConstants, XMLStreamReader}

import org.codehaus.stax2.XMLInputFactory2

import scala.collection.mutable

class XmlReader(val parser: XMLStreamReader) {

  def readPage(): Unit = {

  }

  val openNodes = mutable.Stack

  def readXml(): Unit = {
    while (parser.hasNext) {
      val eventType = parser.next()

      eventType match {
        case XMLStreamConstants.START_ELEMENT =>
          val name = parser.getName.getLocalPart
          name match {
            case "page" => readPage()
            case _ =>
          }
        case XMLStreamConstants.CHARACTERS =>
        //          val text = xmlStreamReader.getText
        //          println(text.substring(0, Math.min(text.size-1, 80)))
        case XMLStreamConstants.END_ELEMENT =>
        //          println("</" + xmlStreamReader.getName.getLocalPart + ">")
        case _ =>
      }
    }
  }

//  def readElement(name: String): String = {
//    if (parser.getEventType != XMLStreamConstants.START_ELEMENT) {
//      while (parser.next() != XMLStreamConstants.START_ELEMENT) { }
//    }
//    while (true) {
//      if (parser.getLocalName == name) {
//        return parser.getElementText
//      }
//      while (parser.next() != XMLStreamConstants.START_ELEMENT) { }
//    }
//  }

}

object XmlReader  {

  val filename = "/mnt/SSD/dump/ukwiki-20140925-pages-articles-multistream.xml"

  val filename_start = "/mnt/SSD/dump/start.xml"

  def parseFile(filename: String) =
  {

    val xmlInputFactory = newXmlInputFactory

    val s = System.currentTimeMillis

//    val file = new File(filename)
    val file = new RandomAccessFile(filename, "r")
    val read =  file.length
    val channel = file.getChannel
    val is = Channels.newInputStream(channel)
    val xmlStreamReader = xmlInputFactory.createXMLStreamReader(is) //.asInstanceOf[XMLStreamReader2]
    new XmlReader(xmlStreamReader).readXml()

    val elapsed = System.currentTimeMillis - s

    val mb = read / (1000.0 * 1000)
    val sec = elapsed / 1000.0

    println(s"Read $read, time ${sec}s , speed ${mb / sec} MB/s")

  }


  def newXmlInputFactory: XMLInputFactory2 = {
    //    System.setProperty("javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl")

    val xmlInputFactory = XMLInputFactory.newInstance().asInstanceOf[XMLInputFactory2]

    //    val xmlInputFactory = XMLInputFactory.newFactory("com.fasterxml.aalto.stax.InputFactoryImpl", null).asInstanceOf[XMLInputFactory2]
    xmlInputFactory.configureForSpeed()
    xmlInputFactory
  }

  def main(args: Array[String]) {
    parseFile(filename)
  }

}