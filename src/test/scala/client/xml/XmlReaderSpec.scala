package client.xml

import org.specs2.mutable.Specification

class XmlReaderSpec extends Specification {

  "parse" should {
    "return no pages" in {

      val s = <mediawiki></mediawiki>.toString()

      //XmlReader.parse()
      1 === 1
    }
  }

}
