package org.scalawiki.bots.museum

import better.files.{File => SFile}
import com.typesafe.config.ConfigFactory
import org.scalawiki.MwBot
import org.scalawiki.bots.FileUtils
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.dto.lists.WlmUa
import FileUtils._

case class Entry(dir: String, article: String, wlmId: String, images: Seq[String])

object Pereiaslav {

  val conf = ConfigFactory.load("pereiaslav.conf")

  val tablePage = conf.getString("table-page")

  val home = conf.getString("home")

  val wlmPage = conf.getString("wlm-page")

  def main(args: Array[String]) {
    subDirs(SFile(home))
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
    val bot = MwBot.get(MwBot.ukWiki)
    val headers = Seq("dir", "article", "wlmId")
    val data = dirs.map(d => Seq(d, d, ""))
    val string = new Table(headers, data).asWiki
    bot.page("User:IlyaBot/test").edit("test", multi = false)
  }

  def wlm(): Seq[Monument] = {
    val ukWiki = MwBot.get(MwBot.ukWiki)
    val text = ukWiki.await(ukWiki.pageText(wlmPage))

    Monument.monumentsFromText(text, wlmPage, WlmUa.templateName, WlmUa).toBuffer
  }

}


