package client.board

import org.scalawiki.dto.User
import client.sweble.{OrderedList, Section, WtTools}
import client.xml.yaidom.YaiDomUtil
import eu.cdevreeze.yaidom.queryapi.HasENameApi._
import eu.cdevreeze.yaidom.simple.{Elem, Node, Text}
import org.scalawiki.MwBot
import org.sweble.wikitext.parser.nodes.WtBody

import scala.collection.JavaConverters._
import scala.collection.immutable.{IndexedSeq, SortedSet}

object SecretaryBot {

  lazy val bot = MwBot.get("ua.wikimedia.org")

  def main(args: Array[String]) {
    val date = "8 січня 2015"
    val title = "Протокол засідання Правління" + " " + date
    val text = bot.await(bot.pageText(title))
    // parseProtocol(title, text, date)
    val protocol = parseProtocolXmlDom(title, text, date)

    val resolution = s"На [[$title|засіданні Правління, що відбулося ${protocol.date}]], Правління вирішило:\n" +
       protocol.resolutions.map { item =>
      s"# ${item.description} ''(${item.votes})''\n" + item.text.lines.map("#" + _).mkString("\n")
    }.mkString("\n") +
    s"\n; Присутні:\n#${protocol.present.mkString("\n#")}"

    println(resolution)
  }

  def parseProtocolXmlDom(title: String, text: String, date: String) = {

    import eu.cdevreeze.yaidom.queryapi.HasENameApi._

    val root = YaiDomUtil.parseToXmlDom(WtTools.wikiToXml(title, text))

    val sections = root \\ withLocalName("section")

    val present = userGroupsInSection(sections, Set("присутні"), _.replaceAll(":", ""))
    val board = new Board(2015, present.head.users)

    val resolutionSections = sectionsByHeader(sections, _.trim.toLowerCase.startsWith("про"))


    val resolutions = resolutionSections.zipWithIndex.flatMap { case (section, index) =>
      val name = sectionHeader(section).head
      val children = sectionChildren(section)

      val wording = sectionsByHeader(children, _.trim.toLowerCase.startsWith("формулювання")).headOption.fold("") { wordingSection =>
        toWiki((wordingSection \ withLocalName("body")).head)
      }

      val voteGroups = userGroupsInSection(children, Set("голосували", "голосування"), s => s"«$s»")

      val votes = Votes(voteGroups)
      if (votes.passed)
        Some(new Resolution(index.toString, date, name, wording, votes, board))
      else
        None
    }

    new BoardProtocol(date, present, resolutions)
  }

  def toWiki(node: Node) = {
    val result = new StringBuilder

    def accumulate(elm: Node): Unit = {
      elm match {
        case text: Text => if (!text.text.trim.isEmpty) result.append(text.text)
        case elem: Elem =>
          elem.children foreach { e => accumulate(e) }
          if (elem.localName == "li") result.append("\n")
      }
    }

    accumulate(node)

    result.toString()
  }



  def userGroupsInSection(sections: Seq[Elem], sectionNames: Set[String], nameMapper: (String => String) = identity): Seq[UserGroup] = {
    sectionsByHeader(sections, s => sectionNames.contains(s.trim.toLowerCase)).headOption
      .fold(Seq.empty[UserGroup]) {
      section =>
        userGroups(sectionChildren(section), nameMapper)
    }
  }

  def userGroups(elems: Seq[Elem], nameMapper: (String => String) = identity): Seq[UserGroup] = {
    val sliding = elems.sliding(2).toIndexedSeq
    sliding.collect {
      case Seq(e1: Elem, e2: Elem) if e1.localName == "dl" && e2.localName == "ol" =>
        val groupName = nameMapper((e1 \\ withLocalName("text")).last.text.trim)
        val users = (e2 \\ withLocalName("text")).map(_.text.trim)
        new UserGroup(groupName, SortedSet(users.map(new User(_, "")):_*))
    }.toSeq
  }

  def sectionHeader(s: Elem): Option[String] = {
    (s \ withLocalName("heading")).flatMap { h =>
      (h \ withLocalName("text")).headOption.map(_.text)
    }.headOption
  }

  def sectionsByHeader(sections: Seq[Elem], f: String => Boolean) = {
    sections.filter(sectionHeader(_).exists(f))
  }

  def sectionChildren(s: Elem): Seq[Elem] = {
    (s \ withLocalName("body")).head.children.collect { case e: Elem => e}
  }


  def parseProtocol(title: String, text: String, date: String) = {
    val cp = WtTools.parse(title, text)
    val page = cp.getPage

    val nodes = page.asScala.toIndexedSeq

    val sections = Section.fromTraversable(nodes).toIndexedSeq
    val present = getPresent(sections)

    val resolutions = sections.filter(_.heading.toLowerCase.startsWith("про"))
    for (resolution <- resolutions) {

      val description = resolution.heading
      val resNodes = resolution.wtSection.getBody.asScala.toIndexedSeq

      val resSections = Section.fromTraversable(resNodes).toIndexedSeq

      val headings = resSections.map(_.heading)

      val wordingSection = resSections.find(_.heading.toLowerCase.trim.startsWith("формулювання"))
      val wordingBody: WtBody = wordingSection.get.wtSection.getBody
      val wording = wordingBody.toString()
      val votesSection = resSections.find(_.heading.toLowerCase.trim.startsWith("голосували"))
    }

    val protocol = new BoardProtocol(date, present, Seq.empty)
    protocol
  }

  def getPresent(sections: IndexedSeq[Section]) = {
    sections.find(_.heading.toLowerCase == "присутні").map {
      present =>
        OrderedList.definitionLists(present.wtSection.getBody.asScala).toIndexedSeq.map {
          case (name, users) => new UserGroup(name, users.map(new User(_, "")).toSet)
        }
    }.getOrElse(Seq.empty)


  }
}
