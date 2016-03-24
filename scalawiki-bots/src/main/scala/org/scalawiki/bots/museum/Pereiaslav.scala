package org.scalawiki.bots.museum

import better.files.{File => SFile}
import com.typesafe.config.ConfigFactory
import org.scalawiki.MwBot
import org.scalawiki.bots.FileUtils
import org.scalawiki.dto.Image
import org.scalawiki.dto.markup.Table
import org.scalawiki.wikitext.TableParser
import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.dto.lists.WlmUa
import FileUtils._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class Entry(dir: String,
                  article: Option[String],
                  wlmId: Option[String],
                  images: Seq[String],
                  descriptions: Seq[String])

object Pereiaslav {

  val conf = ConfigFactory.load("pereiaslav.conf")

  val tablePage = conf.getString("table-page")

  val home = conf.getString("home")

  val wlmPage = conf.getString("wlm-page")

  def ukWiki = MwBot.get(MwBot.ukWiki)

  def main(args: Array[String]) {
    subDirs(SFile(home))
  }

  def getEntries: Future[Seq[Entry]] = {
    for (text <- ukWiki.pageText(tablePage)) yield {
      val table = TableParser.parse(text)
      table.data.toSeq.map { row =>
        val seq = row.toSeq
        val (name, article, wlmId) = (seq(0), seq(1), seq(2))

        val objDir: SFile = SFile(home + name)
        val files = getImages(objDir).map(_.pathAsString)
        val descrs = getImagesDescr(objDir)

        Entry(name, Some(article), Some(wlmId), files, descrs)
      }
    }
  }

  def makeGallery(entries: Seq[Entry]) = {
    entries.map{e =>
      e.article
    }
  }

  def makeGallery(entry: Entry) = {
    s"[[${entry.article}]]" + Image.gallery(entry.images, entry.descriptions)
  }

  def process(dir: SFile) = {
    subDirs(dir)

    val files = getFiles(dir)

    val docs = files.filter(isDoc)

    //  docToHtml(docs)
    val images = getImages(dir)
  }

  def getImages(dir: SFile): Seq[SFile] = {
    val files = getFiles(dir)
    val images = files.filter(isImage)

    images
    //    for (listFile <- files.find(_.name.toLowerCase.startsWith("список"))) yield {
    //
    //      println(s" == ${dir.name}/${listFile.name} (${listFile.pathAsString})== ")

    //      val text = ImageListParser.getDocText(listFile)
    //      val descrs = text.replace("Список ілюстрацій", "").replace("Підписи до ілюстрацій", "").split("\n").map(_.trim).filter(_.nonEmpty)
    //
    //      assert(images.size == descrs.length, s" ${images.size} != ${descrs.length}, ${images.map(_.getName).mkString(",")} != ${descrs.mkString(",")}")
    //      images.zip(descrs)
    //    }
  }

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


