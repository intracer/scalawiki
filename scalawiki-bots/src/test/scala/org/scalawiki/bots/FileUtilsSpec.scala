package org.scalawiki.bots

import java.nio.file.FileSystem

import better.files.Dsl._
import better.files.{File => SFile}
import com.google.common.jimfs.{Configuration, Jimfs}
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach

class FileUtilsSpec extends Specification with BeforeEach {

  var fs: FileSystem = _
  var sep: String = _
  var root: SFile = _

  sequential

  override def before = {
    fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform())
    root = mkdir(SFile(fs.getPath(".")) / "parent")
  }

  "backup name" should {
    "give 1st with ext" in {
      val file = (root / "test.txt").createIfNotExists()

      FileUtils.backupName(file) === "test.txt.1"
    }

    "give 1st no ext" in {
      val file = (root / "test").createIfNotExists()

      FileUtils.backupName(file) === "test.1"
    }
  }

  "writeWithBackup" should {
    "backup file" in {
      val file = (root / "test.txt").overwrite("text1")

      FileUtils.writeWithBackup(file, "text2")

      file.contentAsString === "text2"
      (root / "test.txt.1").contentAsString === "text1"
    }

    "do not backup unchanged file" in {
      val file = (root / "test.txt").overwrite("text1")

      FileUtils.writeWithBackup(file, "text1")

      file.contentAsString === "text1"
      (root / "test.txt.1").exists === false
    }
  }
}