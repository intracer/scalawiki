package org.scalawiki.bots.museum

import java.nio.file.FileSystem

import better.files.Cmds._
import better.files.{File => SFile}
import com.google.common.jimfs.{Configuration, Jimfs}
import org.scalawiki.bots.FileUtils
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach

class PereiaslavSpec extends Specification with BeforeEach {

  var fs: FileSystem = _
  var root: SFile = _

  sequential

  override def before = {
    fs = Jimfs.newFileSystem(Configuration.unix())
    root = mkdir(SFile(fs.getPath("/data")))
  }

  def createFiles(parent: SFile, imageNames: Seq[String]): Seq[SFile] = {
    imageNames.map(n => (parent / n).createIfNotExists())
  }

  "direcory" should {
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

      val list = Pereiaslav.getImages(root)
      list === images
    }

    "get images descriptions from directory" in {
      val imageNames = (1 to 3).map(_ + ".jpg")
      val listName = "list.html"
      createFiles(root, imageNames)

      val descriptions = (1 to 3).map(n => s"$n. Description for $n")
      val html = descriptions.mkString(
        "<html> <body> <h1> Image list </h1> <p>",
        "</p> \n <p>",
        "</p> </body> </html>")

      (root / listName).overwrite(html)

      val list = Pereiaslav.getImagesDescr(root)
      list === descriptions
    }
  }

}
