package client.sweble

import org.sweble.wikitext.engine.{PageId, PageTitle, WtEngineImpl}
import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.nodes.EngProcessedPage
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.parser.nodes.{WtText, WtNode}

import scala.collection.JavaConverters._

object WtTools {

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

    val config: WikiConfig = DefaultConfigEnWp.generate
    val engine: WtEngineImpl = new WtEngineImpl(config)

    // Retrieve a page
    val pageTitle: PageTitle = PageTitle.make(config, title)

    val pageId: PageId = new PageId(pageTitle, -1)

    // Compile the retrieved page
    engine.postprocess(pageId, text, null)
  }


}
