package org.scalawiki.copyvio

import org.scalawiki.util.TestHttpClient
import org.specs2.mutable.Specification

import scala.io.Source

class CopyvioSpec extends Specification {

  "copyvio" should {
    "detect obama mama" in {

      val copyVio = new CopyVio(new TestHttpClient("", Seq.empty))
      val is = getClass.getResourceAsStream("/org/scalawiki/copyvio/barack.json")
      is !== null
      val s = Source.fromInputStream(is).mkString

      val seq = copyVio.parseResponse(s)

      seq.size === 5

      val suspected = seq.filter(_.isSuspected)

      suspected.size === 2

      val most = suspected.head
      most.url === "http://www.whitehouse.gov/administration/president-obama/"

      val least = suspected.last
      least.url === "http://www.usconsulate.org.hk/pas/kids/pr44.htm"

    }
  }

}
