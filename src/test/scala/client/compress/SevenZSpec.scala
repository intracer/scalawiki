package client.compress

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import com.google.common.collect.ImmutableList
import org.specs2.mutable.Specification

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs

class SevenZSpec extends Specification {

  val charset = StandardCharsets.UTF_8

  "compress and uncompress" should {
    "return a page text" in {

      val fs = Jimfs.newFileSystem(Configuration.unix())
//      val foo = fs.getPath("/foo")
//      Files.createDirectory(foo)

      val path = fs.getPath("/text.txt")
      Files.write(path, ImmutableList.of("hello world"), charset)

      val text = Array.fill(10)('a')

//      SevenZ.

      val read = Files.readAllLines(path, charset)

      read === text

    }
  }

}
