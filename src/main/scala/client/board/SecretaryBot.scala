package client.board

import client.MwBot
import client.sweble.{OrderedList, Section, WtTools}

import scala.collection.JavaConverters._

object SecretaryBot {

  val bot = MwBot.get("ua.wikimedia.org")

  def main(args: Array[String]) {
    val date =  "24 грудня 2014"
    val title = "Протокол засідання Правління" + " " + date
    val text = bot.await(bot.pageText(title))
    val cp = WtTools.parse(title, text)
    val page = cp.getPage
    val nodes = page.listIterator().asScala.toIndexedSeq

    val sections = Section.fromTraversable(nodes).toIndexedSeq
    val present = sections.find(_.heading.toLowerCase == "присутні")

   val groups = present.map {
      p =>
        OrderedList.definitionLists(p.wtSection.getBody.listIterator.asScala).toIndexedSeq

    }

    val resolutions = sections.filter(_.heading.toLowerCase.startsWith("про"))
    for (resolution <- resolutions) {
      val resNodes = resolution.wtSection.getBody.listIterator().asScala.toIndexedSeq
      val resSections = Section.fromTraversable(nodes).toIndexedSeq

      val wordingSection = resSections.find(_.heading.toLowerCase.startsWith("формулювання"))
      val wording = wordingSection.get.wtSection.getBody.toString()
      val votesSection = resSections.find(_.heading.toLowerCase.startsWith("голосували"))
    }
  }






}
