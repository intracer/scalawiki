package org.scalawiki.compress

import java.nio.file.Paths

import org.specs2.mutable.Specification

class CompressionSpec extends Specification {

  "compression" should {
    "detect type" in {
      Compression.get(Paths.get("file.bz2")) === Bz2
      Compression.get(Paths.get("file.txt.bz2")) === Bz2

      Compression.get(Paths.get("file.gz")) === Gz
      Compression.get(Paths.get("file.sql.gz")) === Gz

      Compression.get(Paths.get("file.7z")) === SevenZ
      Compression.get(Paths.get("file.xml.7z")) === SevenZ

      Compression.get(Paths.get("file.xml")) === NoCompression
      Compression.get(Paths.get("file.txt")) === NoCompression
      Compression.get(Paths.get("file.sql")) === NoCompression
      Compression.get(Paths.get("file")) === NoCompression
    }
  }

}
