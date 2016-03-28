package org.scalawiki.bots.museum

import java.io.File
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
                 descriptions: Seq[String],
                 text: Option[String] = None)

class Pereiaslav(conf: Config, fs: FileSystem = FileSystems.getDefault) {

  val home = conf.getString("home")

  def ukWiki = MwBot.get(conf.getString("host"))

  val tablePage = conf.getString("table-page")

  val wlmPage = conf.getString("wlm-page")

  def getEntries: Future[Seq[Entry]] = {
    fromWikiTable(tablePage).map {
      tableEntries =>
        tableEntries.map { entry =>

          val objDir = SFile(fs.getPath(s"$home/${entry.dir}"))
          val files = getImages(objDir).map(_.pathAsString)
          val descriptions = getImagesDescr(objDir, files)
          val text = getArticleText(objDir)

          entry.copy(images = files, descriptions = descriptions, text = text)
        }
    }
  }

  def fromWikiTable(tablePage: String): Future[Seq[Entry]] = {
    for (text <- ukWiki.pageText(tablePage)) yield {
      val table = TableParser.parse(text)
      table.data.toSeq.map { row =>
        val seq = row.toSeq
        val (name, article, wlmId) = (seq(0), seq(1), if (seq.size > 2) seq(2) else "")

        Entry(name, Some(article), if (wlmId.trim.nonEmpty) Some(wlmId) else None, Seq.empty, Seq.empty, None)
      }
    }
  }

  def makeEntryGallery(entry: Entry) = {
    Gallery.asHtml(
      entry.images.map(title => new Image(title, url = Some(SFile(title).uri.toString))),
      entry.descriptions
    )
  }

  def getImages(dir: SFile): Seq[SFile] =
    getFiles(dir).filter(isImage)

  def getImagesDescr(dir: SFile, files: Seq[String]): Seq[String] = {
    val docs = getFiles(dir).filter(isDoc).map(_.toJava)
    ImageListParser.docToHtml(docs)
    getFiles(dir).filter(isHtml).sortBy(_.size).headOption.toSeq.flatMap { file =>
      val content = file.contentAsString
      val lines = HtmlParser.trimmedLines(content)
      val filenames = files.map(_.split(File.separator).last.split("\\.").head.toLowerCase)
      lines.filter(l => filenames.exists(l.toLowerCase.startsWith) || l.trim.head.isDigit)
    }
  }

  def getArticleText(dir: SFile): Option[String] = {
    getFiles(dir).filter(isHtml).sortBy(_.size).lastOption.map { file =>
      HtmlParser.htmlText(file.contentAsString)
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
    val pereiaslav = new Pereiaslav(conf)
    pereiaslav.getEntries.map { entries =>
      val galleries = entries.zipWithIndex.map { case (e, i) =>
        s"""<h1 id="e$i">${e.dir}</h1>""" +
          e.text.map(t => s"<br> $t <br>".replace("\n", "<br>")).getOrElse("") +
          pereiaslav.makeEntryGallery(e)
      }

      val navItems = entries.zipWithIndex.map { case (e, i) =>
        s"""<li><a href="e$i.html"> ${e.dir} (${e.images.size}, ${e.descriptions.size}) </a></li>"""
      }

      val head =
        """<head>
          <meta charset="UTF-8">
          <link rel="stylesheet" media="screen" href="main.css">
          </head>"""
      val nav = navItems.mkString(
        """<nav role="navigation" class="table-of-contents">
           <ol>""",
        "\n",
        """</ol>
          </nav>
        """)

      val htmls = galleries.map(g => "<html>" + head + "<body>\n" + nav + g + "\n</body></html>")

      htmls.zipWithIndex.foreach { case (h, i) =>
        SFile(s"${pereiaslav.home}/e$i.html").overwrite(h)
      }
    } onFailure {
      case e =>
        println(e)
    }
  }
}


