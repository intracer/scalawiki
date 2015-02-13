package client.sweble

import java.io.StringReader

import scales.xml.ScalesXml._
import scales.xml._

object ScalesXmlTools {


  def parse(s: String): Unit =  {

    val doc = loadXml(new StringReader(s))

  }

}
