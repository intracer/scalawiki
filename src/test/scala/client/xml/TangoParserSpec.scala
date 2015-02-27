package client.xml

import org.specs2.mutable.Specification

class TangoParserSpec extends Specification{

  val testXml = "<?xml version=\"1.0\" ?><!DOCTYPE element [ <!ELEMENT element (#PCDATA)>]><element " +
  "attr=\"1\" attr2=\"two\"><!--comment-->test&amp;&#x5a;<qual:elem /><el2 attr3 = " +
  "'3three'><![CDATA[sdlgjsh]]><el3 />data<?pi test?></el2></element>"

  "parse" should {
    "process test xml" in {

      val itr = new TangoParser(testXml.toCharArray, true)

      itr.next !== 0

      new String(itr.value) === "xml version=\"1.0\" "
      itr.typ === XmlTokenType.PI

      itr.next !== 0
      new String(itr.value) === "element [ <!ELEMENT element (#PCDATA)>]"
      itr.typ === XmlTokenType.Doctype

      itr.next !== 0
      new String(itr.localName) === "element"
      itr.typ === XmlTokenType.StartElement
      itr.depth === 0

      itr.next !== 0

      new String(itr.localName) === "attr"
      new String(itr.value) === "1"
      itr.next !== 0
      itr.typ === XmlTokenType.Attribute
      new String(itr.localName) === "attr2"
      new String(itr.value) === "two"
      itr.next !== 0
      new String(itr.value) === "comment"
      itr.next !== 0
      new String(itr.rawValue) === "test&amp;&#x5a;"
      itr.next !== 0
      new String(itr.prefix) === "qual"
      new String(itr.localName) === "elem"
      itr.next !== 0
      itr.typ === XmlTokenType.EndEmptyElement
      itr.next !== 0
      new String(itr.localName) === "el2"
      itr.depth === 1
      itr.next !== 0
      new String(itr.localName) === "attr3"
      new String(itr.value) === "3three"
      itr.next !== 0
      new String(itr.rawValue) === "sdlgjsh"
      itr.next !== 0
      new String(itr.localName) === "el3"
      itr.depth === 2
      itr.next !== 0
      itr.typ === XmlTokenType.EndEmptyElement
      itr.next !== 0
      new String(itr.value) === "data"
      itr.next !== 0
      //  itr.qvalue === "pi", itr.qvalue
      //  itr.value === "test"
      new String(itr.rawValue) === "pi test"
      itr.next !== 0
      new String(itr.localName) === "el2"
      itr.next !== 0
      new String(itr.localName) === "element"
      itr.next === 0

    }
  }

}


