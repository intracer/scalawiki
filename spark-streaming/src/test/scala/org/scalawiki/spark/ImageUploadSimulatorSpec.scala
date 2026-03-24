package org.scalawiki.spark

import com.google.common.jimfs.{Configuration, Jimfs}
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.{Files, FileSystem}

class ImageUploadSimulatorSpec extends AnyFunSuite {

  private def makeFs(): FileSystem = Jimfs.newFileSystem(Configuration.unix())

  test("copies source files into target directory") {
    val fs = makeFs()
    val srcDir = fs.getPath("/src")
    Files.createDirectories(srcDir)
    val f1 = srcDir.resolve("a.csv")
    val f2 = srcDir.resolve("b.csv")
    Files.write(f1, "col1,col2\nv1,v2".getBytes)
    Files.write(f2, "col1,col2\nv3,v4".getBytes)

    val targetDir = fs.getPath("/target")
    val sim = new ImageUploadSimulator(
      sourcePaths  = Seq("/src/a.csv", "/src/b.csv"),
      targetDirStr = "/target",
      intervalMs   = 0L,
      fs           = fs
    )
    sim.run()

    assert(Files.exists(targetDir.resolve("a.csv")))
    assert(Files.exists(targetDir.resolve("b.csv")))
    assert(new String(Files.readAllBytes(targetDir.resolve("a.csv"))) == "col1,col2\nv1,v2")
  }

  test("creates target directory if it does not exist") {
    val fs = makeFs()
    val src = fs.getPath("/src/c.csv")
    Files.createDirectories(src.getParent)
    Files.write(src, "data".getBytes)

    val sim = new ImageUploadSimulator(
      sourcePaths  = Seq("/src/c.csv"),
      targetDirStr = "/new/target",
      intervalMs   = 0L,
      fs           = fs
    )
    sim.run()
    assert(Files.exists(fs.getPath("/new/target/c.csv")))
  }

  test("copies files one by one with the configured interval") {
    val fs = makeFs()
    val srcDir = fs.getPath("/src")
    Files.createDirectories(srcDir)
    val files = (1 to 3).map { i =>
      val p = srcDir.resolve(s"f$i.csv")
      Files.write(p, s"data$i".getBytes)
      p
    }

    val copiedAt = scala.collection.mutable.ArrayBuffer[Long]()

    // Subclass to record timestamps
    val sim = new ImageUploadSimulator(
      sourcePaths  = files.map(_.toString),
      targetDirStr = "/target",
      intervalMs   = 100L,
      fs           = fs
    ) {
      override protected def afterCopy(): Unit = copiedAt += System.currentTimeMillis()
    }
    sim.run()

    assert(copiedAt.length == 3)
    // Each copy should be ~100ms apart; allow generous margin for CI
    val gaps = copiedAt.sliding(2).map { s => s(1) - s(0) }.toSeq
    gaps.foreach(gap => assert(gap >= 80L, s"gap $gap ms too short"))
  }
}
