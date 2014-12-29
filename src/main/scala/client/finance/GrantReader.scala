package client.finance

import client.MwBot
import client.sweble.{Section, WtTools}
import org.sweble.wikitext.parser.nodes._
import scala.collection.JavaConverters._

object GrantReader {

  val bot = MwBot.get("meta.wikimedia.org")

  def main(args: Array[String]) {

    val title = "Grants:PEG/WM_UA/Programs_in_Ukraine_2014"
    val text = bot.await(bot.pageText(title))

    val cp = WtTools.parse(title, text)

    val breakDown = WtTools.findNode(cp.getPage, node => Section(node).exists(_.heading.toLowerCase == "detailed breakdown")).get

    val table = WtTools.findNode(breakDown, _.isInstanceOf[WtTable]).get.asInstanceOf[WtTable]

    val rows = table.getBody.get(0).asInstanceOf[WtTableImplicitTableBody].getBody.asScala.map(_.asInstanceOf[WtTableRow])
    //sections.find()
    val headers = rows.head.getBody.asScala.map(_.asInstanceOf[WtTableHeader])

    val titles = headers.map(WtTools.getText)

    println(titles.toString())

    for (row <- rows.tail if row.getBody.size() > 0) {

      def getContent(row: WtTableRow, cell:Int): String = WtTools.getText(row.getBody.get(cell).asInstanceOf[WtTableCell].getBody).trim

      val funded = row.getBody.size() match {
        case 5 => getContent(row, 2)
        case 9 => getContent(row, 6)
      }

      val description = row.getBody.size() match {
        case 5 => "== " + getContent(row, 0)
        case 9 => "   " + (0 to 1).map(getContent(row,_)).mkString(" ")
      }

      println(s"$description - $funded")
    }
  }

}
