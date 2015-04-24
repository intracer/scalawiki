package org.scalawiki.xml

import java.io.ByteArrayInputStream

import org.specs2.mutable.Specification

class SkippingInputStreamSpec extends Specification{

  "SkippingInputStream" should {

    "read nothing" in {
      val input = "1234567890".getBytes
      val sis = new SkippingInputStream(new ByteArrayInputStream(input), Seq(Block(0, 0)))
      val buf = Array.fill[Byte](10)(0)
      sis.read(buf) === -1
      buf === Array.fill[Byte](10)(0)
    }

    "read all stream to large buf" in {
      val input = "1234567890".getBytes
      val sis = new SkippingInputStream(new ByteArrayInputStream(input), Seq(Block(0, 10)))
      val buf = Array.fill[Byte](10)(0)
      sis.read(buf) === 10
      buf === input
      sis.read(buf) === -1
    }

    "read all stream with block larger than input" in {
      val input = "1234567890".getBytes
      val sis = new SkippingInputStream(new ByteArrayInputStream(input), Seq(Block(0, 20)))
      val buf = Array.fill[Byte](10)(0)
      sis.read(buf) === 10
      buf === input
      sis.read(buf) === -1
    }

    "read all stream to small buf" in {
      val input = "1234567890"
      val sis = new SkippingInputStream(new ByteArrayInputStream(input.getBytes), Seq(Block(0, 10)))
      val buf = Array.fill[Byte](5)(0)
      sis.read(buf) === 5
      new String(buf) === input.slice(0, 5)

      sis.read(buf) === 5
      new String(buf) === input.slice(5, 10)
      sis.read(buf) === -1
    }

    "read first two blocks to large buf" in {
      val input = ("1234567890" + "qwertyuiop" + "asdfghjkl" + "zxcvbnm").getBytes
      val sis = new SkippingInputStream(new ByteArrayInputStream(input), Seq(Block(0, 10), Block(10, 10)))
      val buf = Array.fill[Byte](20)('-')
      sis.read(buf) === 20
      new String(buf) === "1234567890" +  "qwertyuiop"
      sis.read(buf) === -1
    }

    "read first two blocks to small buf" in {
      val input = ("1234567890" + "qwertyuiop" + "asdfghjkl" + "zxcvbnm").getBytes
      val sis = new SkippingInputStream(new ByteArrayInputStream(input), Seq(Block(0, 10), Block(10, 10)))
      val buf = Array.fill[Byte](5)('-')

      sis.read(buf) === 5
      new String(buf) === "12345"
      sis.read(buf) === 5
      new String(buf) === "67890"
      sis.read(buf) === 5
      new String(buf) === "qwert"
      sis.read(buf) === 5
      new String(buf) === "yuiop"
      sis.read(buf) === -1
    }

    "read last two blocks" in {
      val input = ("1234567890" + "qwertyuiop" + "asdfghjkl" + "zxcvbnm").getBytes
      val sis = new SkippingInputStream(new ByteArrayInputStream(input), Seq(Block(20, 9), Block(29, 7)))
      val buf = Array.fill[Byte](20)('-')
      sis.read(buf) === 16
      new String(buf) === "asdfghjkl" +  "zxcvbnm" + "----"
      sis.read(buf) === -1
    }

    "read odd blocks" in {
      val input = ("1234567890" + "qwertyuiop" + "asdfghjkl" + "zxcvbnm").getBytes
      val sis = new SkippingInputStream(new ByteArrayInputStream(input), Seq(Block(0, 10), Block(20, 9)))
      val buf = Array.fill[Byte](20)('-')
      sis.read(buf) === 19
      new String(buf) === "1234567890" +  "asdfghjkl" + "-"
      sis.read(buf) === -1
    }

    "read odd blocks in small buff" in {
      val input = ("1234567890" + "qwertyuiop" + "asdfghjkl" + "zxcvbnm").getBytes
      val sis = new SkippingInputStream(new ByteArrayInputStream(input), Seq(Block(0, 10), Block(20, 9)))
      val buf = Array.fill[Byte](5)('-')
      sis.read(buf) === 5
      new String(buf) === "12345"
      sis.read(buf) === 5
      new String(buf) === "67890"
      sis.read(buf) === 5
      new String(buf) === "asdfg"
      sis.read(buf) === 4
      new String(buf) === "hjklg"
      sis.read(buf) === -1
    }

    "read even blocks" in {
      val input = ("1234567890" + "qwertyuiop" + "asdfghjkl" + "zxcvbnm").getBytes
      val sis = new SkippingInputStream(new ByteArrayInputStream(input), Seq(Block(10, 10), Block(29, 7)))
      val buf = Array.fill[Byte](20)('-')
      sis.read(buf) === 17
      new String(buf) === "qwertyuiop" + "zxcvbnm" + "---"
    }

    "read even blocks in small buf" in {
      val input = ("1234567890" + "qwertyuiop" + "asdfghjkl" + "zxcvbnm").getBytes
      val sis = new SkippingInputStream(new ByteArrayInputStream(input), Seq(Block(10, 10), Block(29, 7)))
      val buf = Array.fill[Byte](5)('-')
      sis.read(buf) === 5
      new String(buf) === "qwert"
      sis.read(buf) === 5
      new String(buf) === "yuiop"
      sis.read(buf) === 5
      new String(buf) === "zxcvb"
      sis.read(buf) === 2
      new String(buf) === "nmcvb"
      sis.read(buf) === -1
    }
  }

}
