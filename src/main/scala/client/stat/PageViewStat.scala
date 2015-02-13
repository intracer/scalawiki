package client.stat

import java.io._
import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import client.HttpClientImpl
import client.xml.yaidom.YaiDomUtil
import eu.cdevreeze.yaidom.queryapi.HasENameApi._

object PageViewStat {

  val system = ActorSystem()
  val http = new HttpClientImpl(system)

  def main(args: Array[String]) {
    val pagecountsLinks = list(2014, 1)

  }

  def list(year: Int, month: Int) = {

    val padded = String.format("%02d", new Integer(month))
    val url = s"http://dumps.wikimedia.org/other/pagecounts-raw/$year/$year-$padded/"

    val filename: String = s"pagecounts_$year-$padded.html"
    //http.download(url, filename)

    val xml = new String(Files.readAllBytes(Paths.get(filename)))

    val dom = YaiDomUtil.parseToXmlDom(xml)

    val anchors = dom \\ withLocalName("a")
    val hrefs = anchors.flatMap(_.findAttributeByLocalName("href"))

    val pagecountsLinks = hrefs.filter(s => s.startsWith(s"pagecounts-$year$padded") && s.endsWith(".gz"))
    pagecountsLinks

  }

    def read7z(path: String, bufferSize: Int = 1 << 14)  = {

        val pos = new PipedOutputStream()
        val pis = new PipedInputStream(pos, bufferSize)


      }
    }

  class SevenZipReaderThread(path: String, pos: OutputStream, bufferSize: Int = 1 << 14) extends Thread {

    override def run(): Unit = {
  }

}
