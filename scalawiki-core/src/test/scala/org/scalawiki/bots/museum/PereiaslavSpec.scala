package org.scalawiki.bots.museum

import java.io.File

import org.specs2.mutable.Specification

class PereiaslavSpec extends Specification {

  "Pereiaslav" should {
    "return list of object groups" in {

      val files = Pereiaslav.subDirs(new File(Pereiaslav.home))

      files.length === 55
    }

    "return list of object images" in {

      val files = Pereiaslav.subDirs(new File(Pereiaslav.home))

      ok
      //files.flatMap(_.images).size === 550
    }


  }
}
