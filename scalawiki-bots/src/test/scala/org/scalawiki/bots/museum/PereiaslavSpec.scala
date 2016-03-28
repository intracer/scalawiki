package org.scalawiki.bots.museum

import java.nio.file.FileSystem

import better.files.Cmds._
import better.files.{File => SFile}
import com.google.common.jimfs.{Configuration, Jimfs}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalawiki.MwBot
import org.scalawiki.bots.FileUtils
import org.scalawiki.dto.markup.Table
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import org.specs2.mock.Mockito

import spray.util.pimpFuture

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

class PereiaslavSpec extends Specification with BeforeEach with Mockito {

  var fs: FileSystem = _
  var root: SFile = _
  val ukWiki = "uk.wikipedia.org"

  sequential

  def pereiaslav(cfg: Config = config()) = new Pereiaslav(cfg, fs)

  def config(host: String = ukWiki,
             tablePage: String = "tablePage",
             home: String = "/data",
             wlmPage: String = ""): Config = {
    val map = Map(
      "host" -> host,
      "table-page" -> tablePage,
      "home" -> home,
      "wlm-page" -> wlmPage
    )
    ConfigFactory.parseMap(map.asJava)
  }

  override def before = {
    fs = Jimfs.newFileSystem(Configuration.unix())
    root = mkdir(SFile(fs.getPath("/data")))
  }

  def createFiles(parent: SFile, names: Seq[String]): Seq[SFile] = {
    names.map(n => (parent / n).createIfNotExists())
  }

  "directory" should {
    "list objects" in {
      val names = (1 to 3) map ("Object" + _)
      val dirs = names.map(n => mkdir(root / n))

      val list = FileUtils.subDirs(root.path)
      list === dirs
    }

    "get images from directory" in {
      val imageNames = (1 to 3).map(_ + ".jpg") :+ "4.tif"
      val otherNames = Seq("1.doc", "2.docx", "3.html")
      val images = createFiles(root, imageNames)
      createFiles(root, otherNames)

      val list = pereiaslav().getImages(root)
      list === images
    }

    "get images descriptions from directory" in {
      val listName = "list.html"
      val imageNames = (1 to 3).map(_ + ".jpg")
      createFiles(root, imageNames)

      val descriptions = (1 to 3).map(n => s"$n. Description for $n")
      val html = descriptions.mkString(
        "<html> <body> <h1> Image list </h1> <p>",
        "</p> \n <p>",
        "</p> </body> </html>")

      (root / listName).overwrite(html)

      val list = pereiaslav().getImagesDescr(root, imageNames)
      list === descriptions
    }
  }

  "fromWikiTable" should {
    "return entries" in {

      val tablePage = new Table(
        Seq("name", "article", "wlmId"),
        Seq(
          Seq("name1", "article1", "wlmId1"),
          Seq("name2", "article2", "")
        )
      ).asWiki

      val dirs = Seq("name1", "name2").map(n => mkdir(root / n))

      mockBot(ukWiki, "tablePage", tablePage)

      val entries = pereiaslav().getEntries.await
      entries.size === 2
      entries.head === Entry("name1", Some("article1"), Some("wlmId1"), Seq.empty, Seq.empty)
      entries.last === Entry("name2", Some("article2"), None, Seq.empty, Seq.empty)
    }

    "return entries with images" in {

      val tablePage = new Table(
        Seq("name", "article", "wlmId"),
        Seq(
          Seq("name1", "article1", "wlmId1"),
          Seq("name2", "article2", "")
        )
      ).asWiki

      val dirs = Seq("name1", "name2").map(n => mkdir(root / n))

      dirs.zipWithIndex.foreach { case (dir, i) =>
        val names = (1 to 3).map(j => j + i * 10 + ".jpg")
        createFiles(dir, names)
      }

      mockBot(ukWiki, "tablePage", tablePage)

      val entries = pereiaslav().getEntries.await
      entries.size === 2
      entries.head === Entry("name1", Some("article1"), Some("wlmId1"), (1 to 3).map(i => s"/data/name1/$i.jpg"), Seq.empty)
      entries.last === Entry("name2", Some("article2"), None, (11 to 13).map(i => s"/data/name2/$i.jpg"), Seq.empty)
    }
  }

  def mockBot(host: String, page: String, text: String): Future[MwBot] = {
    val ukWiki = mock[MwBot]
    ukWiki.pageText(page) returns Future {
      text
    }

    MwBot.cache(host) {
      ukWiki
    }
  }
}
