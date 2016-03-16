package org.scalawiki.bots.museum

import better.files.{File => SFile}
import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.dto.lists.WlmUa


case class Entry(dir: String, article: String, wlmId: String, images: Seq[String])


object Pereiaslav {

  val tablePage = "Користувач:IlyaBot/Національний історико-етнографічний заповідник «Переяслав»"

  val home = "/mnt/SSD/GLAM/VIKIPEDIIA_NIEZ_PEREYIASLAV"

  val wlmPage = "Вікіпедія:Вікі любить пам'ятки/Київська область/Переяслав-Хмельницький"

  def main(args: Array[String]) {
    subDirs(SFile(home))

  }


  def subDirs(dir: SFile): Seq[SFile] = {
    dir.list.filter(_.isDirectory).toSeq.sortBy(_.name)
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
    val images = files.filter(isImage).sortBy(_.name)

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

  def getFiles(dir: SFile): Seq[SFile] = {
    dir.list.filter(_.isRegularFile).toSeq.sortBy(_.name)
  }

  def makeTable(dirs: Seq[String]) = {
    val bot = MwBot.get(MwBot.ukWiki)
    val headers = Seq("dir", "article", "wlmId")
    val data = dirs.map(d => Seq(d, d, ""))
    val string = new Table(headers, data).asWiki
    bot.page("User:IlyaBot/test").edit("test", multi = false)
  }


  def isImage(f: SFile): Boolean = isExt(f, Set(".jpg", ".tif"))

  def isDoc(f: SFile): Boolean = isExt(f, Set(".doc", ".docx"))

  def isHtml(f: SFile): Boolean = isExt(f, Set(".htm", ".html"))


  def isExt(file: SFile, ext: Set[String]): Boolean = {
    ext.contains(file.extension.getOrElse("."))
  }


  def wlm(): Seq[Monument] = {
    val ukWiki = MwBot.get(MwBot.ukWiki)
    val text = ukWiki.await(ukWiki.pageText(wlmPage))

    Monument.monumentsFromText(text, wlmPage, WlmUa.templateName, WlmUa).toBuffer
  }

}


