package client.sweble

import org.joda.time.DateTime
import org.sweble.wikitext.engine.{PageId, PageTitle, WtEngineImpl}
import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.nodes.EngProcessedPage
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.parser.nodes.{WtText, WtNode}
import org.sweble.wom3.swcadapter.AstToWomConverter
import org.sweble.wom3.util.Wom3Toolbox

import scala.collection.JavaConverters._

object WtTools {

  val config: WikiConfig = DefaultConfigEnWp.generate

  def findNode(node: WtNode, predicate: (WtNode => Boolean)): Option[WtNode] =
    node match {
      case p if predicate(p) => Some(p)
      case p => p.asScala.view.flatMap(child => findNode(child, predicate)).headOption
    }

  def getText(node: WtNode): String =
    node match {
      case p: WtText => p.getContent
      case p => p.asScala.map(getText).mkString
    }


  def parse(title: String, text: String): EngProcessedPage ={

    val engine: WtEngineImpl = new WtEngineImpl(config)

    val pageId = getPageId(title)

    // Compile the retrieved page
    engine.postprocess(pageId, text, null)

  }

  def getPageId(title: String): PageId = {
    // Retrieve a page
    val pageTitle: PageTitle = PageTitle.make(config, title)

    val pageId: PageId = new PageId(pageTitle, -1)
    pageId
  }

  def toXml(page: EngProcessedPage, title: String): String = {

    val pageId = getPageId(title)

    val womDoc = AstToWomConverter.convert(
      config,
      pageId.getTitle,
      "Mr. Tester",
      DateTime.parse("2012-12-07T12:15:30.000+01:00"),
      page.getPage)

    val xml = Wom3Toolbox.printWom(womDoc)

    xml

  }

  def wikiToXml(title: String, text: String): String = {
    toXml(parse(title, text), title)
  }


}
