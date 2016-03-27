package org.scalawiki.bots.museum

import java.nio.file.{FileSystem, FileSystems}

import better.files.{File => SFile}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalawiki.MwBot
import org.scalawiki.bots.FileUtils
import org.scalawiki.dto.Image
import org.scalawiki.dto.markup.{Gallery, Table}
import org.scalawiki.wikitext.TableParser
import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.dto.lists.WlmUa
import FileUtils._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Upload entry
  *
  * @param dir          directory
  * @param article      wikipedia article
  * @param wlmId        Wiki Loves Monuments Id
  * @param images       images in the directory
  * @param descriptions image descriptions
  */
case class Entry(dir: String,
                 article: Option[String],
                 wlmId: Option[String],
                 images: Seq[String],
                 descriptions: Seq[String])

class Pereiaslav(conf: Config, fs: FileSystem = FileSystems.getDefault) {

  val home = conf.getString("home")

  def ukWiki = MwBot.get(conf.getString("host"))

  val tablePage = conf.getString("table-page")

  val wlmPage = conf.getString("wlm-page")

  def getEntries: Future[Seq[Entry]] = {
    for (tableEntries <- fromWikiTable(tablePage)) yield {
      tableEntries.map { entry =>

        val objDir = SFile(fs.getPath(s"$home/${entry.dir}"))
        val files = getImages(objDir).map(_.pathAsString)
        val descriptions = getImagesDescr(objDir)

        entry.copy(images = files, descriptions = descriptions)
      }
    }
  }

  def fromWikiTable(tablePage: String): Future[Seq[Entry]] = {
    for (text <- ukWiki.pageText(tablePage)) yield {
      val table = TableParser.parse(text)
      table.data.toSeq.map { row =>
        val seq = row.toSeq
        val (name, article, wlmId) = (seq(0), seq(1), seq(2))

        Entry(name, Some(article), if (wlmId.trim.nonEmpty) Some(wlmId) else None, Seq.empty, Seq.empty)
      }
    }
  }

  def makeEntryGallery(entry: Entry) = {
    Gallery.asHtml(
      entry.images.map(title => new Image(title, url = Some(SFile(s"$home/${entry.dir}/$title").uri.toString))),
      entry.descriptions
    )
  }

  def getImages(dir: SFile): Seq[SFile] =
    getFiles(dir).filter(isImage)

  def getImagesDescr(dir: SFile): Seq[String] = {
    getFiles(dir).find(isHtml).toSeq.flatMap { file =>
      val content = file.contentAsString
      val lines = HtmlParser.trimmedLines(content)
      lines.filter(_.head.isDigit)
    }
  }

  def makeTable(dirs: Seq[String]) = {

    val headers = Seq("dir", "article", "wlmId")
    val data = dirs.map(d => Seq(d, d, ""))
    val string = new Table(headers, data).asWiki
    ukWiki.page("User:IlyaBot/test").edit("test", multi = false)
  }

  def wlm(): Seq[Monument] = {
    val ukWiki = MwBot.get(MwBot.ukWiki)
    val text = ukWiki.await(ukWiki.pageText(wlmPage))

    Monument.monumentsFromText(text, wlmPage, WlmUa.templateName, WlmUa).toBuffer
  }

}

object Pereiaslav {

  def main(args: Array[String]) {
    val conf = ConfigFactory.load("pereiaslav.conf")
    new Pereiaslav(conf).getEntries
  }
}


