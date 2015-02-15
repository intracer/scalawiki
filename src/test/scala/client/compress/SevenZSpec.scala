package client.compress

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import com.google.common.jimfs.{Configuration, Jimfs}
import org.specs2.mutable.Specification

import scala.collection.JavaConverters._

class SevenZSpec extends Specification {

  val charset = StandardCharsets.UTF_8

  "compress and uncompress" should {
    "return a page text" in {

      val fs = Jimfs.newFileSystem(Configuration.unix())
//      val foo = fs.getPath("/foo")
//      Files.createDirectory(foo)

      val path = fs.getPath("/text.txt")
      Files.write(path, "hello world".getBytes(charset))

      val text = "hello world"

      val read = Files.readAllLines(path, charset).asScala.toSeq

      read === Seq(text)

    }
  }

}
