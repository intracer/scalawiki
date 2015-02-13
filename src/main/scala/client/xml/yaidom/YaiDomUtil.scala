package client.xml.yaidom

import java.io.ByteArrayInputStream

import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.simple.Elem

object YaiDomUtil {

  def parseToXmlDom(xml: String): Elem = {
    import eu.cdevreeze.yaidom.parse

    val docParser = parse.DocumentParserUsingSax.newInstance()

    val doc = docParser.parse(new ByteArrayInputStream(xml.getBytes))

    doc.documentElement
  }

  def parsehtml(xml: String): Elem = {
    import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl

    val docParser = DocumentParserUsingSax.newInstance(new SAXFactoryImpl)

    val doc = docParser.parse(new ByteArrayInputStream(xml.getBytes))

    doc.documentElement
  }

}
