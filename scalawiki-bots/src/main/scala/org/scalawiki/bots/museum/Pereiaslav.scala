package org.scalawiki.bots.museum

import java.io.File

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
    listDir(new File(home))
  }

  def listDir(dir: File): Unit = {
    val dirs = subDirs(dir)

    makeTable(dirs.map(_.getName))

//    dirs.foreach(process)
  }

  def subDirs(dir: File): Array[File] = {
    dir.listFiles.filter(_.isDirectory).sorted
  }

  def process(dir: File) = {
    listDir(dir)

    val files = dir.listFiles.filter(_.isFile).sorted

    val docs = files.filter(isDoc)

    //  docToHtml(docs)
    val images = getImages(dir, files).getOrElse(Seq.empty)
  }

  def getImages(dir: File, files: Seq[File]) = {
    val images = files.filter(isImage).sorted

    for (listFile <- files.find(_.getName.toLowerCase.startsWith("список"))) yield {

      println(s" == ${dir.getName}/${listFile.getName} (${listFile.getAbsolutePath})== ")

//      val text = ImageListParser.getDocText(listFile)
//      val descrs = text.replace("Список ілюстрацій", "").replace("Підписи до ілюстрацій", "").split("\n").map(_.trim).filter(_.nonEmpty)
//
//      assert(images.size == descrs.length, s" ${images.size} != ${descrs.length}, ${images.map(_.getName).mkString(",")} != ${descrs.mkString(",")}")
//      images.zip(descrs)
    }
  }

  def makeTable(dirs: Seq[String]) = {
    val bot = MwBot.get(MwBot.ukWiki)
    val headers = Seq("dir", "article", "wlmId")
    val data = dirs.map(d => Seq(d, d, ""))
    val string = new Table(headers, data).asWiki
    bot.page("User:IlyaBot/test").edit("test", multi = false)
  }


  def isImage(f: File): Boolean = {
    val name = f.getName.toLowerCase
    name.endsWith(".jpg") || name.endsWith(".tif")
  }

  def isDoc(f: File): Boolean = {
    val name = f.getName.toLowerCase
    name.endsWith(".doc") || name.endsWith(".docx")
  }

  def wlm(): Seq[Monument] = {
    val ukWiki = MwBot.get(MwBot.ukWiki)
    val text = ukWiki.await(ukWiki.pageText(wlmPage))

    Monument.monumentsFromText(text, wlmPage, WlmUa.templateName, WlmUa).toBuffer
  }

}


