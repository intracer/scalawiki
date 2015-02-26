package client.xml

import java.io.{BufferedInputStream, FileInputStream, RandomAccessFile}
import java.nio.ByteBuffer
import java.nio.channels.Channels
import javax.xml.stream.{XMLInputFactory, XMLStreamConstants}

import org.apache.kafka.common.message.KafkaLZ4BlockInputStream
import org.codehaus.stax2.XMLInputFactory2

import scala.collection.mutable

class XmlReaderTest {

  def time(f: => Unit) = {
    val s = System.currentTimeMillis
    f
    System.currentTimeMillis - s
  }

}

object XmlReaderTest {

  val filename = "/mnt/SSD/dump/ukwiki-20140925-pages-articles-multistream.xml"
  val filename_sm = "/mnt/SSD/dump/SMALLPART"
  val filename_serbian = "/mnt/SSD/dump/serbian.xml"
  val filename_latin = "/mnt/SSD/dump/latin.xml"
  val filename_start = "/mnt/SSD/dump/start.xml"

  val filename_lzh4 = "/mnt/SSD/dump/ukwiki-20140925-pages-articles-multistream.xml.lz4"

  val filename_index = "/mnt/SSD/dump/ukwiki-20140925-pages-articles-multistream-index.txt"
  val filename_mkv = "/home/ilya/Downloads/Bacheha-Ye aseman [BDRip 1920x1080 x264-10 2xRus(AC3) Per (FLAC)].mkv"

  def read1(filename: String) = {
    val s = System.currentTimeMillis

    val is = new BufferedInputStream(new FileInputStream(filename))
    val buffer = new Array[Byte](1024)

    val read = Stream.continually(is.read(buffer)).takeWhile(-1 !=).map(_.toLong).sum

    val elapsed = System.currentTimeMillis - s

    val mb = read / (1000.0 * 1000)
    val sec = elapsed / 1000.0

    println(s"Read $read, time ${sec}s , speed ${mb / sec} MB/s")
  }

  def read2(filename: String) = {
    val s = System.currentTimeMillis

    val is = new FileInputStream(filename)
    val ch = is.getChannel
    val array = new Array[Byte](256 * 1024)
    val bb = ByteBuffer.wrap(array)

    val read = Stream.continually(ch.read(bb)).takeWhile(-1 !=).map {
      bb.clear(); _.toLong
    }.sum

    val elapsed = System.currentTimeMillis - s

    val mb = read / (1000.0 * 1000)
    val sec = elapsed / 1000.0

    println(s"Read $read, time ${sec}s , speed ${mb / sec} MB/s")
  }

  def read3(filename: String) = {
    val s = System.currentTimeMillis

    val f = new RandomAccessFile(filename, "r")
    val size = 128 * 1024
    val array = new Array[Byte](size)
    var checkSum = 0L

    val read = Stream.continually(f.read(array, 0, size)).takeWhile(-1 !=).map {
      checkSum += array.sum; _.toLong
    }.sum

    val elapsed = System.currentTimeMillis - s

    val mb = read / (1000.0 * 1000)
    val sec = elapsed / 1000.0

    println(s"Read $read, time ${sec}s , speed ${mb / sec} MB/s")
  }

  def read4(filename: String) = {
    val s = System.currentTimeMillis

    val channel = new RandomAccessFile(filename, "r").getChannel
    val is = Channels.newInputStream(channel)
    val size = 128 * 1024

    val buffer = new Array[Byte](size)

    val read = Stream.continually(is.read(buffer)).takeWhile(-1 !=).map(_.toLong).sum

    val elapsed = System.currentTimeMillis - s

    val mb = read / (1000.0 * 1000)
    val sec = elapsed / 1000.0

    println(s"Read $read, time ${sec}s , speed ${mb / sec} MB/s")
  }

  def readAndUnlz4(filename: String) = {
    val s = System.currentTimeMillis

    val channel = new RandomAccessFile(filename, "r").getChannel
    val is = new KafkaLZ4BlockInputStream(Channels.newInputStream(channel))
    val size = 128 * 1024

    val buffer = new Array[Byte](size)

    val read = Stream.continually(is.read(buffer)).takeWhile(-1 !=).map(_.toLong).sum

    val elapsed = System.currentTimeMillis - s

    val mb = read / (1000.0 * 1000)
    val sec = elapsed / 1000.0

    println(s"Read $read, time ${sec}s , speed ${mb / sec} MB/s")
  }


  def readAndParseXml(filename: String) = {

    //    System.setProperty("javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl")

    val xmlInputFactory = XMLInputFactory.newInstance().asInstanceOf[XMLInputFactory2]

    //    val xmlInputFactory = XMLInputFactory.newFactory("com.fasterxml.aalto.stax.InputFactoryImpl", null).asInstanceOf[XMLInputFactory2]
    xmlInputFactory.configureForSpeed()

    val s = System.currentTimeMillis

    val file = new RandomAccessFile(filename, "r")
    val read = file.length
    val channel = file.getChannel
    val is = Channels.newReader(channel, "UTF-8")
//   val is = Channels.newInputStream(channel)

    val xmlStreamReader = xmlInputFactory.createXMLStreamReader(is)//.asInstanceOf[XMLStreamReader2]

    val stack = mutable.Stack[String]()


    while (xmlStreamReader.hasNext) {
      val eventType = xmlStreamReader.next()

      eventType match {
        case XMLStreamConstants.START_ELEMENT =>
//      println("<" + xmlStreamReader.getName.getLocalPart + ">")
        case XMLStreamConstants.CHARACTERS =>
//          val text = xmlStreamReader.getText
//          println(text.substring(0, Math.min(text.size-1, 80)))
        case XMLStreamConstants.END_ELEMENT =>
//          println("</" + xmlStreamReader.getName.getLocalPart + ">")
        case _ =>
      }
    }

    val elapsed = System.currentTimeMillis - s

    val mb = read / (1000.0 * 1000)
    val sec = elapsed / 1000.0

    println(s"Read $read, time ${sec}s , speed ${mb / sec} MB/s")

  }


  def main(args: Array[String]) {
    readAndParseXml(filename)
  }
}
